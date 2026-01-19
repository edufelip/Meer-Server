package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.moderation.ModerationStatus;
import com.edufelip.meer.domain.repo.ImageModerationRepository;
import com.edufelip.meer.dto.ImageModerationDto;
import com.edufelip.meer.dto.ManualReviewRequest;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.moderation.BlockedImageCleanupService;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Dashboard API for manual content moderation review. Admin-only endpoint for reviewing images
 * flagged by the NSFW detection system.
 */
@RestController
@RequestMapping("/dashboard/moderation")
public class ModerationDashboardController {

  private final ImageModerationRepository imageModerationRepository;
  private final AuthUserResolver authUserResolver;
  private final BlockedImageCleanupService cleanupService;

  public ModerationDashboardController(
      ImageModerationRepository imageModerationRepository,
      AuthUserResolver authUserResolver,
      BlockedImageCleanupService cleanupService) {
    this.imageModerationRepository = imageModerationRepository;
    this.authUserResolver = authUserResolver;
    this.cleanupService = cleanupService;
  }

  /**
   * List images flagged for manual review
   *
   * @param page Page number (0-based)
   * @param pageSize Number of items per page
   * @return Paginated list of flagged images
   */
  @GetMapping("/flagged")
  public PageResponse<ImageModerationDto> listFlaggedImages(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestHeader("Authorization") String authHeader) {

    var user = authUserResolver.requireUser(authHeader);
    if (user.getRole() != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }

    Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
    Pageable pageable = PageRequest.of(page, pageSize, sort);

    var flaggedPage = imageModerationRepository.findFlaggedForReview(pageable);
    var items = flaggedPage.getContent().stream().map(Mappers::toDto).toList();

    return new PageResponse<>(items, page, flaggedPage.hasNext());
  }

  /**
   * List all moderation records by status
   *
   * @param status Moderation status to filter by
   * @param page Page number (0-based)
   * @param pageSize Number of items per page
   * @return Paginated list of moderation records
   */
  @GetMapping
  public PageResponse<ImageModerationDto> listByStatus(
      @RequestParam(required = false) ModerationStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestHeader("Authorization") String authHeader) {

    var user = authUserResolver.requireUser(authHeader);
    if (user.getRole() != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }

    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
    Pageable pageable = PageRequest.of(page, pageSize, sort);

    var moderationPage =
        status != null
            ? imageModerationRepository.findByStatus(status, pageable)
            : imageModerationRepository.findAll(pageable);

    var items = moderationPage.getContent().stream().map(Mappers::toDto).toList();

    return new PageResponse<>(items, page, moderationPage.hasNext());
  }

  /**
   * Get a single moderation record by ID
   *
   * @param id Moderation record ID
   * @return Moderation record details
   */
  @GetMapping("/{id}")
  public ImageModerationDto getModerationById(
      @PathVariable Long id, @RequestHeader("Authorization") String authHeader) {

    var user = authUserResolver.requireUser(authHeader);
    if (user.getRole() != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    var moderation =
        imageModerationRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Moderation record not found"));

    return Mappers.toDto(moderation);
  }

  /**
   * Submit manual review decision for a flagged image
   *
   * @param id Moderation record ID
   * @param request Review decision (MANUALLY_APPROVED or MANUALLY_REJECTED) and notes
   * @return Updated moderation record
   */
  @PatchMapping("/{id}/review")
  public ImageModerationDto submitManualReview(
      @PathVariable Long id,
      @RequestBody ManualReviewRequest request,
      @RequestHeader("Authorization") String authHeader) {

    var user = authUserResolver.requireUser(authHeader);
    if (user.getRole() != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    var moderation =
        imageModerationRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Moderation record not found"));

    if (moderation.getStatus() != ModerationStatus.FLAGGED_FOR_REVIEW) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Only flagged images can be manually reviewed");
    }

    if (request == null || request.decision() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Decision is required for manual review");
    }

    if (request.decision() != ModerationStatus.MANUALLY_APPROVED
        && request.decision() != ModerationStatus.MANUALLY_REJECTED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Decision must be MANUALLY_APPROVED or MANUALLY_REJECTED");
    }

    moderation.setStatus(request.decision());
    moderation.setReviewedAt(Instant.now());
    moderation.setReviewedBy(user.getEmail());
    moderation.setReviewNotes(request.notes());

    var updated = imageModerationRepository.save(moderation);
    if (updated.getStatus() == ModerationStatus.MANUALLY_REJECTED) {
      cleanupService.cleanupImmediately(updated);
    }
    return Mappers.toDto(updated);
  }

  /**
   * Get moderation statistics
   *
   * @return Statistics about moderation queue
   */
  @GetMapping("/stats")
  public ResponseEntity<?> getStats(@RequestHeader("Authorization") String authHeader) {

    var user = authUserResolver.requireUser(authHeader);
    if (user.getRole() != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    long pending = imageModerationRepository.countByStatus(ModerationStatus.PENDING);
    long processing = imageModerationRepository.countByStatus(ModerationStatus.PROCESSING);
    long flagged = imageModerationRepository.countByStatus(ModerationStatus.FLAGGED_FOR_REVIEW);
    long blocked = imageModerationRepository.countByStatus(ModerationStatus.BLOCKED);
    long approved = imageModerationRepository.countByStatus(ModerationStatus.APPROVED);
    long failed = imageModerationRepository.countByStatus(ModerationStatus.FAILED);

    var stats =
        java.util.Map.of(
            "pending", pending,
            "processing", processing,
            "flaggedForReview", flagged,
            "blocked", blocked,
            "approved", approved,
            "failed", failed,
            "total", pending + processing + flagged + blocked + approved + failed);

    return ResponseEntity.ok(stats);
  }
}
