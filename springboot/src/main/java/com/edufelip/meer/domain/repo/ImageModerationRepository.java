package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.moderation.EntityType;
import com.edufelip.meer.core.moderation.ImageModeration;
import com.edufelip.meer.core.moderation.ModerationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageModerationRepository extends JpaRepository<ImageModeration, Long> {

  /**
   * Find moderation record by image URL
   *
   * @param imageUrl The image URL
   * @return Optional moderation record
   */
  Optional<ImageModeration> findByImageUrl(String imageUrl);

  /**
   * Find moderation record by image URL and entity
   *
   * @param imageUrl The image URL
   * @param entityType Type of entity
   * @param entityId Entity identifier
   * @return Optional moderation record
   */
  Optional<ImageModeration> findByImageUrlAndEntityTypeAndEntityId(
      String imageUrl, EntityType entityType, String entityId);

  /**
   * Find moderation records by entity type and ID
   *
   * @param entityType Type of entity (store photo, avatar, guide content)
   * @param entityId Entity identifier
   * @return List of moderation records
   */
  List<ImageModeration> findByEntityTypeAndEntityId(EntityType entityType, String entityId);

  /**
   * Find all images with a specific status
   *
   * @param status Moderation status
   * @param pageable Pagination parameters
   * @return Page of moderation records
   */
  Page<ImageModeration> findByStatus(ModerationStatus status, Pageable pageable);

  /**
   * Find images flagged for manual review
   *
   * @param pageable Pagination parameters
   * @return Page of flagged images
   */
  @Query(
      "SELECT im FROM ImageModeration im WHERE im.status = 'FLAGGED_FOR_REVIEW' ORDER BY im.createdAt ASC")
  Page<ImageModeration> findFlaggedForReview(Pageable pageable);

  /**
   * Lock and return pending moderation IDs for processing (multi-instance safe)
   *
   * @param limit Maximum number of records to return
   * @return List of pending moderation IDs
   */
  @Query(
      value =
          "SELECT id FROM image_moderation WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  List<Long> lockPendingIdsForProcessing(@Param("limit") int limit);

  /**
   * Lock and return failed moderation IDs for retry (multi-instance safe)
   *
   * @param maxRetries Maximum retry attempts
   * @param limit Maximum number of records to return
   * @return List of failed moderation IDs eligible for retry
   */
  @Query(
      value =
          "SELECT id FROM image_moderation WHERE status = 'FAILED' AND retry_count < :maxRetries ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  List<Long> lockFailedIdsForRetry(@Param("maxRetries") int maxRetries, @Param("limit") int limit);

  /**
   * Bulk update status by IDs
   *
   * @param status New moderation status
   * @param ids Moderation IDs
   * @return number of rows updated
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE ImageModeration im SET im.status = :status WHERE im.id IN :ids")
  int updateStatusByIds(@Param("status") ModerationStatus status, @Param("ids") Collection<Long> ids);

  /**
   * Count images by status
   *
   * @param status Moderation status
   * @return Count of images
   */
  long countByStatus(ModerationStatus status);

  /**
   * Find all blocked images within a date range
   *
   * @param start Start date
   * @param end End date
   * @return List of blocked images
   */
  @Query(
      "SELECT im FROM ImageModeration im WHERE im.status = 'BLOCKED' AND im.processedAt BETWEEN :start AND :end ORDER BY im.processedAt DESC")
  List<ImageModeration> findBlockedBetween(
      @Param("start") Instant start, @Param("end") Instant end);

  /**
   * Find moderation records that require cleanup
   *
   * @param statuses Target statuses
   * @param pageable Pagination parameters
   * @return Page of moderation records needing cleanup
   */
  @Query(
      "SELECT im FROM ImageModeration im WHERE im.status IN :statuses AND im.cleanupAt IS NULL ORDER BY im.processedAt DESC")
  Page<ImageModeration> findForCleanup(
      @Param("statuses") Collection<ModerationStatus> statuses, Pageable pageable);

  /**
   * Check if an image URL is already being moderated
   *
   * @param imageUrl The image URL
   * @return true if exists, false otherwise
   */
  boolean existsByImageUrl(String imageUrl);
}
