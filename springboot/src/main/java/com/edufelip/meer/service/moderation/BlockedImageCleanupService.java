package com.edufelip.meer.service.moderation;

import com.edufelip.meer.config.ModerationProperties;
import com.edufelip.meer.core.moderation.EntityType;
import com.edufelip.meer.core.moderation.ImageModeration;
import com.edufelip.meer.core.moderation.ModerationStatus;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.ImageModerationRepository;
import com.edufelip.meer.domain.repo.ThriftStorePhotoRepository;
import com.edufelip.meer.service.GcsStorageService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that handles cleanup actions for blocked images. Deletes images from GCS and removes
 * references from database entities.
 */
@Service
public class BlockedImageCleanupService {

  private static final Logger log = LoggerFactory.getLogger(BlockedImageCleanupService.class);

  private final ImageModerationRepository imageModerationRepository;
  private final ThriftStorePhotoRepository storePhotoRepository;
  private final AuthUserRepository authUserRepository;
  private final GuideContentRepository guideContentRepository;
  private final GcsStorageService gcsStorageService;
  private final ModerationProperties properties;

  public BlockedImageCleanupService(
      ImageModerationRepository imageModerationRepository,
      ThriftStorePhotoRepository storePhotoRepository,
      AuthUserRepository authUserRepository,
      GuideContentRepository guideContentRepository,
      GcsStorageService gcsStorageService,
      ModerationProperties properties) {
    this.imageModerationRepository = imageModerationRepository;
    this.storePhotoRepository = storePhotoRepository;
    this.authUserRepository = authUserRepository;
    this.guideContentRepository = guideContentRepository;
    this.gcsStorageService = gcsStorageService;
    this.properties = properties;
  }

  /**
   * Scheduled task that processes newly blocked images and removes them from storage and database.
   * Runs every 2 minutes to ensure timely removal of inappropriate content.
   */
  @Scheduled(fixedDelay = 120_000, initialDelay = 30_000)
  public void cleanupBlockedImages() {
    try {
      if (!properties.isEnabled()) {
        return;
      }
      var blocked =
          imageModerationRepository.findForCleanup(
              List.of(ModerationStatus.BLOCKED, ModerationStatus.MANUALLY_REJECTED),
              org.springframework.data.domain.PageRequest.of(0, 50));

      if (!blocked.isEmpty()) {
        log.info("Found {} blocked images to clean up", blocked.getTotalElements());

        for (ImageModeration moderation : blocked.getContent()) {
          try {
            cleanupBlockedImage(moderation);
          } catch (Exception e) {
            log.error(
                "Failed to cleanup blocked image: id={}, url={}",
                moderation.getId(),
                moderation.getImageUrl(),
                e);
          }
        }
      }
    } catch (Exception e) {
      log.error("Error in cleanupBlockedImages scheduled task", e);
    }
  }

  /**
   * Cleans up a single blocked image by: 1. Deleting the file from GCS 2. Removing database
   * references (store photo, avatar, etc.) 3. Marking the moderation record as processed
   *
   * @param moderation The blocked image moderation record
   */
  private void cleanupBlockedImage(ImageModeration moderation) {
    if (moderation.getCleanupAt() != null) {
      return;
    }
    String imageUrl = moderation.getImageUrl();
    EntityType entityType = moderation.getEntityType();
    String entityId = moderation.getEntityId();

    log.info(
        "Cleaning up blocked image: id={}, url={}, entityType={}, entityId={}",
        moderation.getId(),
        imageUrl,
        entityType,
        entityId);

    // Delete from GCS
    try {
      gcsStorageService.deleteByUrl(imageUrl);
      log.info("Deleted blocked image from GCS: url={}", imageUrl);
    } catch (Exception e) {
      log.error("Failed to delete blocked image from GCS: url={}", imageUrl, e);
      // Continue with database cleanup even if GCS deletion fails
    }

    cleanupDatabaseReferencesAndMark(moderation.getId(), imageUrl, entityType, entityId);
  }

  @Transactional
  protected void cleanupDatabaseReferencesAndMark(
      Long moderationId, String imageUrl, EntityType entityType, String entityId) {
    // Remove database references based on entity type
    switch (entityType) {
      case STORE_PHOTO -> cleanupStorePhoto(imageUrl, entityId);
      case USER_AVATAR -> cleanupUserAvatar(imageUrl, entityId);
      case GUIDE_CONTENT_IMAGE -> cleanupGuideContentImage(imageUrl, entityId);
      default -> log.warn("Unknown entity type for cleanup: {}", entityType);
    }

    imageModerationRepository
        .findById(moderationId)
        .ifPresent(
            moderation -> {
              if (moderation.getCleanupAt() == null) {
                moderation.setCleanupAt(Instant.now());
                imageModerationRepository.save(moderation);
              }
            });
  }

  private void cleanupStorePhoto(String imageUrl, String entityId) {
    try {
      // entityId for store photos is the photo ID
      Integer photoId = Integer.parseInt(entityId);
      var photoOpt = storePhotoRepository.findById(photoId);

      if (photoOpt.isPresent()) {
        ThriftStorePhoto photo = photoOpt.get();
        if (photo.getUrl().equals(imageUrl)) {
          storePhotoRepository.delete(photo);
          log.info("Deleted blocked store photo from database: photoId={}", photoId);
        }
      }
    } catch (Exception e) {
      log.error("Failed to cleanup store photo: entityId={}, url={}", entityId, imageUrl, e);
    }
  }

  private void cleanupUserAvatar(String imageUrl, String entityId) {
    try {
      // entityId for avatars is the user UUID
      java.util.UUID userId = java.util.UUID.fromString(entityId);
      var userOpt = authUserRepository.findById(userId);

      if (userOpt.isPresent()) {
        var user = userOpt.get();
        if (imageUrl.equals(user.getPhotoUrl())) {
          user.setPhotoUrl(null);
          authUserRepository.save(user);
          log.info("Cleared blocked avatar from user: userId={}", userId);
        }
      }
    } catch (Exception e) {
      log.error("Failed to cleanup user avatar: entityId={}, url={}", entityId, imageUrl, e);
    }
  }

  private void cleanupGuideContentImage(String imageUrl, String entityId) {
    try {
      // entityId for guide content is the content ID
      Integer contentId = Integer.parseInt(entityId);
      var contentOpt = guideContentRepository.findById(contentId);

      if (contentOpt.isPresent()) {
        var content = contentOpt.get();
        if (imageUrl.equals(content.getImageUrl())) {
          content.setImageUrl(null);
          guideContentRepository.save(content);
          log.info("Cleared blocked image from guide content: contentId={}", contentId);
        }
      }
    } catch (Exception e) {
      log.error(
          "Failed to cleanup guide content image: entityId={}, url={}", entityId, imageUrl, e);
    }
  }

  public void cleanupImmediately(ImageModeration moderation) {
    if (moderation == null) {
      return;
    }
    cleanupBlockedImage(moderation);
  }
}
