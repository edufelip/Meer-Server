package com.edufelip.meer.service.moderation;

import com.edufelip.meer.config.ModerationProperties;
import com.edufelip.meer.core.moderation.EntityType;
import com.edufelip.meer.core.moderation.ImageModeration;
import com.edufelip.meer.core.moderation.ModerationStatus;
import com.edufelip.meer.domain.repo.ImageModerationRepository;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that orchestrates image moderation decisions based on NSFW scores. Implements the policy
 * for auto-approval, flagging for review, and auto-blocking.
 */
@Service
public class ModerationPolicyService {

  private static final Logger log = LoggerFactory.getLogger(ModerationPolicyService.class);

  private final ModerationProperties properties;
  private final NsfwInferenceService nsfwInferenceService;
  private final ImageModerationRepository imageModerationRepository;
  private final Storage storage;
  private final String bucket;

  public ModerationPolicyService(
      ModerationProperties properties,
      ObjectProvider<NsfwInferenceService> nsfwInferenceServiceProvider,
      ImageModerationRepository imageModerationRepository,
      Storage storage,
      @org.springframework.beans.factory.annotation.Value("${storage.gcs.bucket}") String bucket) {
    this.properties = properties;
    this.nsfwInferenceService = nsfwInferenceServiceProvider.getIfAvailable();
    this.imageModerationRepository = imageModerationRepository;
    this.storage = storage;
    this.bucket = bucket;
  }

  /**
   * Enqueues an image for moderation. Creates a PENDING record that will be picked up by the
   * worker.
   *
   * @param imageUrl Public URL of the image
   * @param entityType Type of entity (store photo, avatar, etc.)
   * @param entityId Unique identifier for the entity
   * @return The created ImageModeration record
   */
  @Transactional
  public ImageModeration enqueueForModeration(
      String imageUrl, EntityType entityType, String entityId) {
    if (!properties.isEnabled()) {
      log.debug("NSFW moderation disabled; skipping enqueue for url={}", imageUrl);
      return null;
    }
    // Check if already exists
    var existing =
        imageModerationRepository.findByImageUrlAndEntityTypeAndEntityId(
            imageUrl, entityType, entityId);
    if (existing.isPresent()) {
      log.debug("Image already queued for moderation: {}", imageUrl);
      return existing.get();
    }

    ImageModeration moderation = new ImageModeration();
    moderation.setImageUrl(imageUrl);
    moderation.setEntityType(entityType);
    moderation.setEntityId(entityId);
    moderation.setStatus(ModerationStatus.PENDING);

    ImageModeration saved = imageModerationRepository.save(moderation);
    log.info(
        "Enqueued image for moderation: id={}, url={}, entityType={}, entityId={}",
        saved.getId(),
        imageUrl,
        entityType,
        entityId);

    return saved;
  }

  /**
   * Processes a single image moderation record. Downloads the image, runs inference, applies
   * policy, and updates the database.
   *
   * @param moderation The moderation record to process
   * @return The updated moderation record
   */
  @Transactional
  public ImageModeration processImage(ImageModeration moderation) {
    try {
      if (!properties.isEnabled()) {
        moderation.setStatus(ModerationStatus.APPROVED);
        moderation.setProcessedAt(java.time.Instant.now());
        return imageModerationRepository.save(moderation);
      }
      moderation.setStatus(ModerationStatus.PROCESSING);
      moderation = imageModerationRepository.save(moderation);

      log.info(
          "Processing image moderation: id={}, url={}",
          moderation.getId(),
          moderation.getImageUrl());

      // Download image from GCS
      BufferedImage image = downloadImageFromGcs(moderation.getImageUrl());
      if (image == null) {
        throw new IOException("Failed to download or decode image");
      }

      // Run NSFW inference
      if (nsfwInferenceService == null) {
        throw new IllegalStateException("NSFW inference service unavailable");
      }
      double nsfwScore = nsfwInferenceService.inferNsfwProbability(image);
      moderation.setNsfwScore(nsfwScore);

      // Apply moderation policy
      applyModerationPolicy(moderation, nsfwScore);

      moderation.setProcessedAt(java.time.Instant.now());
      moderation = imageModerationRepository.save(moderation);

      log.info(
          "Completed image moderation: id={}, score={}, status={}",
          moderation.getId(),
          nsfwScore,
          moderation.getStatus());

      return moderation;

    } catch (Exception e) {
      log.error("Failed to process image moderation: id={}", moderation.getId(), e);
      moderation.setStatus(ModerationStatus.FAILED);
      moderation.setFailureReason(e.getMessage());
      moderation.incrementRetryCount();
      moderation.setProcessedAt(java.time.Instant.now());
      return imageModerationRepository.save(moderation);
    }
  }

  /**
   * Applies the moderation policy based on NSFW score thresholds. - Score < allowThreshold:
   * APPROVED - Score >= allowThreshold and < reviewThreshold: FLAGGED_FOR_REVIEW - Score >=
   * reviewThreshold: BLOCKED
   */
  private void applyModerationPolicy(ImageModeration moderation, double nsfwScore) {
    double allowThreshold = properties.getThreshold().getAllow();
    double reviewThreshold = properties.getThreshold().getReview();

    if (nsfwScore < allowThreshold) {
      // Safe content - auto-approve
      moderation.setStatus(ModerationStatus.APPROVED);
    } else if (nsfwScore < reviewThreshold) {
      // Gray zone - flag for manual review
      // Note: Per business requirement, bikini photos should not be blocked
      // This threshold allows for human judgment
      moderation.setStatus(ModerationStatus.FLAGGED_FOR_REVIEW);
    } else {
      // High NSFW score - auto-block (explicit nudity, violence, etc.)
      moderation.setStatus(ModerationStatus.BLOCKED);
    }
  }

  /**
   * Downloads an image from GCS and decodes it to BufferedImage
   *
   * @param imageUrl Public URL or GCS URL
   * @return BufferedImage or null if download fails
   */
  private BufferedImage downloadImageFromGcs(String imageUrl) {
    try {
      // Extract bucket and blob name from URL
      // Format: https://storage.googleapis.com/{bucket}/{blob}
      // or gs://{bucket}/{blob}
      String blobName = extractBlobName(imageUrl);
      if (blobName == null) {
        log.error("Failed to extract blob name from URL: {}", imageUrl);
        return null;
      }

      Blob blob = storage.get(bucket, blobName);
      if (blob == null || !blob.exists()) {
        log.error("Blob not found in GCS: bucket={}, blob={}", bucket, blobName);
        return null;
      }

      byte[] content = blob.getContent();
      return ImageIO.read(new ByteArrayInputStream(content));

    } catch (Exception e) {
      log.error("Failed to download image from GCS: {}", imageUrl, e);
      return null;
    }
  }

  /**
   * Extracts the blob name (object key) from a GCS URL
   *
   * @param imageUrl Full URL
   * @return Blob name or null if extraction fails
   */
  private String extractBlobName(String imageUrl) {
    try {
      if (imageUrl.startsWith("gs://")) {
        // gs://bucket/path/to/file -> path/to/file
        URI uri = URI.create(imageUrl);
        return uri.getPath().substring(1); // Remove leading /
      } else if (imageUrl.contains("storage.googleapis.com/")) {
        // https://storage.googleapis.com/bucket/path/to/file -> path/to/file
        String[] parts = imageUrl.split("storage.googleapis.com/");
        if (parts.length > 1) {
          String afterHost = parts[1];
          int firstSlash = afterHost.indexOf('/');
          if (firstSlash > 0) {
            return afterHost.substring(firstSlash + 1);
          }
        }
      }
      return null;
    } catch (Exception e) {
      log.error("Failed to parse GCS URL: {}", imageUrl, e);
      return null;
    }
  }
}
