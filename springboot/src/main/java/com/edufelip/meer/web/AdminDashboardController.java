package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.DashboardCommentDto;
import com.edufelip.meer.dto.DashboardStoreSummaryDto;
import com.edufelip.meer.dto.GuideContentCommentDto;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.DashboardAdminAuthorizer;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/dashboard")
public class AdminDashboardController {

  private final DashboardAdminAuthorizer adminAuthorizer;
  private final ThriftStoreRepository thriftStoreRepository;
  private final GuideContentRepository guideContentRepository;
  private final GuideContentCommentRepository guideContentCommentRepository;
  private final GuideContentEngagementService guideContentEngagementService;
  private final GuideContentModerationService guideContentModerationService;

  public AdminDashboardController(
      DashboardAdminAuthorizer adminAuthorizer,
      ThriftStoreRepository thriftStoreRepository,
      GuideContentRepository guideContentRepository,
      GuideContentCommentRepository guideContentCommentRepository,
      GuideContentEngagementService guideContentEngagementService,
      GuideContentModerationService guideContentModerationService) {
    this.adminAuthorizer = adminAuthorizer;
    this.thriftStoreRepository = thriftStoreRepository;
    this.guideContentRepository = guideContentRepository;
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.guideContentEngagementService = guideContentEngagementService;
    this.guideContentModerationService = guideContentModerationService;
  }

  @GetMapping("/stores")
  public PageResponse<DashboardStoreSummaryDto> listStores(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(name = "search", required = false) String search,
      @RequestParam(defaultValue = "newest") String sort) {
    adminAuthorizer.requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    String term = search;
    Sort s =
        "oldest".equalsIgnoreCase(sort)
            ? Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt")
            : Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
    Pageable pageable = PageRequest.of(page, pageSize, s);
    var pageRes =
        (term != null && !term.isBlank())
            ? thriftStoreRepository.search(term.trim(), pageable)
            : thriftStoreRepository.findAll(pageable);
    List<DashboardStoreSummaryDto> items =
        pageRes.getContent().stream()
            .map(
                ts ->
                    new DashboardStoreSummaryDto(
                        ts.getId(),
                        ts.getName(),
                        ts.getAddressLine(),
                        ts.getIsOnlineStore(),
                        ts.getCreatedAt()))
            .toList();
    return new PageResponse<>(items, page, pageRes.hasNext());
  }

  @GetMapping("/contents")
  public PageResponse<GuideContentDto> listContents(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "newest") String sort) {
    adminAuthorizer.requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    Sort.Direction direction =
        "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort s = Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    Pageable pageable = PageRequest.of(page, pageSize, s);
    var slice =
        (q != null && !q.isBlank())
            ? guideContentRepository.searchSummariesActive(q, pageable)
            : guideContentRepository.findAllSummariesActive(pageable);
    var items = slice.getContent().stream().map(Mappers::toDto).toList();
    var ids = items.stream().map(GuideContentDto::id).toList();
    var engagement = guideContentEngagementService.getEngagement(ids, null);
    var enriched =
        items.stream()
            .map(
                item -> {
                  var summary =
                      engagement.getOrDefault(
                          item.id(),
                          new GuideContentEngagementService.EngagementSummary(0L, 0L, false));
                  return Mappers.withCounts(
                      item, summary.likeCount(), summary.commentCount(), summary.likedByMe());
                })
            .toList();
    return new PageResponse<>(enriched, page, slice.hasNext());
  }

  @GetMapping("/contents/{id}")
  public GuideContentDto getContent(
      @RequestHeader("Authorization") String authHeader, @PathVariable Integer id) {
    adminAuthorizer.requireAdmin(authHeader);
    var content =
        guideContentRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    var engagement = guideContentEngagementService.getEngagement(List.of(content.getId()), null);
    var summary =
        engagement.getOrDefault(
            content.getId(), new GuideContentEngagementService.EngagementSummary(0L, 0L, false));
    return Mappers.toDto(content, summary.likeCount(), summary.commentCount(), summary.likedByMe());
  }

  @DeleteMapping("/contents/{id}")
  public org.springframework.http.ResponseEntity<Void> deleteContent(
      @RequestHeader("Authorization") String authHeader, @PathVariable Integer id) {
    AuthUser admin = adminAuthorizer.requireAdmin(authHeader);
    var content =
        guideContentRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    guideContentModerationService.softDeleteContent(content, admin, null);
    return org.springframework.http.ResponseEntity.noContent().build();
  }

  @PostMapping("/contents/{id}/restore")
  public GuideContentDto restoreContent(
      @RequestHeader("Authorization") String authHeader, @PathVariable Integer id) {
    adminAuthorizer.requireAdmin(authHeader);
    var content =
        guideContentRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    var restored = guideContentModerationService.restoreContent(content);
    var engagement = guideContentEngagementService.getEngagement(List.of(restored.getId()), null);
    var summary =
        engagement.getOrDefault(
            restored.getId(), new GuideContentEngagementService.EngagementSummary(0L, 0L, false));
    return Mappers.toDto(
        restored, summary.likeCount(), summary.commentCount(), summary.likedByMe());
  }

  @GetMapping("/contents/{id}/comments")
  public PageResponse<GuideContentCommentDto> listContentComments(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable Integer id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    adminAuthorizer.requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    if (guideContentRepository.findById(id).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found");
    }
    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
    Pageable pageable = PageRequest.of(page, pageSize, sort);
    var comments = guideContentCommentRepository.findByContentId(id, pageable);
    var items = comments.getContent().stream().map(Mappers::toDto).toList();
    return new PageResponse<>(items, page, comments.hasNext());
  }

  @DeleteMapping("/contents/{id}/comments/{commentId}")
  public org.springframework.http.ResponseEntity<Void> deleteContentComment(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable Integer id,
      @PathVariable Integer commentId) {
    adminAuthorizer.requireAdmin(authHeader);
    var comment =
        guideContentCommentRepository
            .findById(commentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    if (comment.getContent() == null || !comment.getContent().getId().equals(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
    }
    guideContentModerationService.hardDeleteComment(comment);
    return org.springframework.http.ResponseEntity.noContent().build();
  }

  @GetMapping("/comments")
  public PageResponse<DashboardCommentDto> listComments(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Integer contentId,
      @RequestParam(required = false) UUID storeId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    adminAuthorizer.requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
    Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
    Sort.Direction direction =
        "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort sortSpec = Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    Pageable pageable = PageRequest.of(page, pageSize, sortSpec);
    String term = (search != null && !search.isBlank()) ? search.trim() : null;
    var pageRes =
        guideContentCommentRepository.findDashboardComments(
            contentId, storeId, fromInstant, toInstant, term, pageable);
    var items = pageRes.getContent().stream().map(Mappers::toDashboardCommentDto).toList();
    return new PageResponse<>(items, page, pageRes.hasNext());
  }

}
