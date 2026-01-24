package com.edufelip.meer.service.moderation;

import com.edufelip.meer.config.ModerationProperties;
import com.edufelip.meer.core.moderation.ImageModeration;
import com.edufelip.meer.core.moderation.ModerationStatus;
import com.edufelip.meer.domain.repo.ImageModerationRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Background worker that processes pending image moderation tasks. Uses a dedicated thread pool and
 * scheduled polling to ensure images are verified without blocking user requests.
 */
@Service
@ConditionalOnProperty(
    name = "moderation.nsfw.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ImageModerationWorker {

  private static final Logger log = LoggerFactory.getLogger(ImageModerationWorker.class);

  private static final int BATCH_SIZE = 10;
  private static final int MAX_RETRIES = 3;

  private final ImageModerationRepository imageModerationRepository;
  private final ModerationPolicyService moderationPolicyService;
  private final ModerationProperties properties;
  private final TransactionTemplate transactionTemplate;

  public ImageModerationWorker(
      ImageModerationRepository imageModerationRepository,
      ModerationPolicyService moderationPolicyService,
      ModerationProperties properties,
      PlatformTransactionManager transactionManager) {
    this.imageModerationRepository = imageModerationRepository;
    this.moderationPolicyService = moderationPolicyService;
    this.properties = properties;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * Scheduled task that polls for PENDING moderation records and processes them. Runs every 30
   * seconds to maintain low latency without overwhelming the system.
   */
  @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
  public void processPendingImages() {
    if (!properties.isEnabled()) {
      return;
    }
    try {
      List<Long> pendingIds = claimPendingIds();

      if (!pendingIds.isEmpty()) {
        log.info("Found {} pending images for moderation", pendingIds.size());

        for (Long moderationId : pendingIds) {
          processImageAsync(moderationId);
        }
      }
    } catch (Exception e) {
      log.error("Error in processPendingImages scheduled task", e);
    }
  }

  /**
   * Scheduled task that retries FAILED moderation records (up to MAX_RETRIES). Runs every 5 minutes
   * to give failed tasks time to recover from transient issues.
   */
  @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
  public void retryFailedImages() {
    if (!properties.isEnabled()) {
      return;
    }
    try {
      List<Long> failedIds = claimFailedIdsForRetry();

      if (!failedIds.isEmpty()) {
        log.info("Found {} failed images for retry", failedIds.size());
      }
    } catch (Exception e) {
      log.error("Error in retryFailedImages scheduled task", e);
    }
  }

  /**
   * Processes a single image moderation task asynchronously in the dedicated moderation thread
   * pool. This ensures image verification doesn't block user-facing API requests.
   *
   * @param moderation The moderation record to process
   */
  @Async("moderationTaskExecutor")
  public void processImageAsync(Long moderationId) {
    try {
      Optional<ImageModeration> moderation = imageModerationRepository.findById(moderationId);
      moderation.ifPresentOrElse(
          moderationPolicyService::processImage,
          () -> log.warn("Moderation record not found for id={}", moderationId));
    } catch (Exception e) {
      log.error("Unexpected error processing image moderation: id={}", moderationId, e);
    }
  }

  /**
   * Immediately submits an image for async processing. Used when images are uploaded and need
   * immediate verification.
   *
   * @param moderation The moderation record to process
   */
  public void submitForProcessing(ImageModeration moderation) {
    log.info("Submitting image for immediate processing: id={}", moderation.getId());
    processImageAsync(moderation.getId());
  }

  private List<Long> claimPendingIds() {
    List<Long> ids =
        transactionTemplate.execute(
            status -> {
              List<Long> lockedIds =
                  imageModerationRepository.lockPendingIdsForProcessing(BATCH_SIZE);
              if (!lockedIds.isEmpty()) {
                imageModerationRepository.updateStatusByIds(ModerationStatus.PROCESSING, lockedIds);
              }
              return lockedIds;
            });
    return ids == null ? List.of() : ids;
  }

  private List<Long> claimFailedIdsForRetry() {
    List<Long> ids =
        transactionTemplate.execute(
            status -> {
              List<Long> lockedIds =
                  imageModerationRepository.lockFailedIdsForRetry(MAX_RETRIES, BATCH_SIZE);
              if (!lockedIds.isEmpty()) {
                imageModerationRepository.updateStatusByIds(ModerationStatus.PENDING, lockedIds);
              }
              return lockedIds;
            });
    return ids == null ? List.of() : ids;
  }
}
