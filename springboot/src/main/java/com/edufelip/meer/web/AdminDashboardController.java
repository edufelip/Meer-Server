package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.*;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GcsStorageService;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/dashboard")
public class AdminDashboardController {

  private final TokenProvider tokenProvider;
  private final AuthUserRepository authUserRepository;
  private final ThriftStoreRepository thriftStoreRepository;
  private final GuideContentRepository guideContentRepository;
  private final GuideContentCommentRepository guideContentCommentRepository;
  private final StoreFeedbackRepository storeFeedbackRepository;
  private final GcsStorageService gcsStorageService;
  private final GuideContentEngagementService guideContentEngagementService;
  private final GuideContentModerationService guideContentModerationService;

  public AdminDashboardController(
      TokenProvider tokenProvider,
      AuthUserRepository authUserRepository,
      ThriftStoreRepository thriftStoreRepository,
      GuideContentRepository guideContentRepository,
      GuideContentCommentRepository guideContentCommentRepository,
      StoreFeedbackRepository storeFeedbackRepository,
      GcsStorageService gcsStorageService,
      GuideContentEngagementService guideContentEngagementService,
      GuideContentModerationService guideContentModerationService) {
    this.tokenProvider = tokenProvider;
    this.authUserRepository = authUserRepository;
    this.thriftStoreRepository = thriftStoreRepository;
    this.guideContentRepository = guideContentRepository;
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.storeFeedbackRepository = storeFeedbackRepository;
    this.gcsStorageService = gcsStorageService;
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
    requireAdmin(authHeader);
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
                        ts.getId(), ts.getName(), ts.getAddressLine(), ts.getCreatedAt()))
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
    requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    Sort.Direction direction =
        "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort s = Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    Pageable pageable = PageRequest.of(page, pageSize, s);
    var slice =
        (q != null && !q.isBlank())
            ? guideContentRepository.searchSummaries(q, pageable)
            : guideContentRepository.findAllSummaries(pageable);
    var items = slice.getContent();
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
    requireAdmin(authHeader);
    var content =
        guideContentRepository
            .findById(id)
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
    AuthUser admin = requireAdmin(authHeader);
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
    requireAdmin(authHeader);
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
    requireAdmin(authHeader);
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
    requireAdmin(authHeader);
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
    requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    Instant fromInstant =
        from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
    Instant toInstant =
        to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
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

  @GetMapping("/users")
  public PageResponse<AdminUserDto> listUsers(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "newest") String sort) {
    requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    Sort s =
        "oldest".equalsIgnoreCase(sort)
            ? Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt")
            : Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
    Pageable pageable = PageRequest.of(page, pageSize, s);
    var pageRes =
        (q != null && !q.isBlank())
            ? authUserRepository.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                q, q, pageable)
            : authUserRepository.findAll(pageable);
    List<AdminUserDto> items =
        pageRes.getContent().stream()
            .map(
                u ->
                    new AdminUserDto(
                        u.getId().toString(),
                        u.getDisplayName(),
                        u.getEmail(),
                        (u.getRole() != null ? u.getRole() : Role.USER).name(),
                        u.getCreatedAt(),
                        u.getPhotoUrl()))
            .toList();
    return new PageResponse<>(items, page, pageRes.hasNext());
  }

  @GetMapping("/users/{id}")
  public ProfileDto getUser(
      @RequestHeader("Authorization") String authHeader, @PathVariable java.util.UUID id) {
    requireAdmin(authHeader);
    var user =
        authUserRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return Mappers.toProfileDto(user, true);
  }

  @DeleteMapping("/users/{id}")
  public org.springframework.http.ResponseEntity<Void> deleteUser(
      @RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
    AuthUser admin = requireAdmin(authHeader);
    var target =
        authUserRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    // Clear owned thrift store links before deleting stores to satisfy FK constraints
    if (target.getOwnedThriftStore() != null) {
      target.setOwnedThriftStore(null);
      authUserRepository.save(target);
    }

    deleteOwnedStores(target);

    // Drop favorites join entries
    target.getFavorites().clear();
    authUserRepository.save(target);

    // Delete avatar assets, if any
    if (target.getPhotoUrl() != null) {
      if (!deleteLocalIfUploads(target.getPhotoUrl())) {
        gcsStorageService.deleteByUrl(target.getPhotoUrl());
      }
    }

    storeFeedbackRepository.deleteByUserId(target.getId());
    authUserRepository.delete(target);
    return org.springframework.http.ResponseEntity.noContent().build();
  }

  private AuthUser requireAdmin(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
    }
    // Reuse admin user loaded by DashboardAdminGuardFilter when present
    AuthUser user = resolveAdminFromRequest();

    Role effectiveRole = user.getRole() != null ? user.getRole() : Role.USER;
    if (effectiveRole != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
    }
    return user;
  }

  private AuthUser resolveAdminFromRequest() {
    try {
      Object cached =
          RequestContextHolder.currentRequestAttributes()
              .getAttribute("adminUser", RequestAttributes.SCOPE_REQUEST);
      if (cached instanceof AuthUser cachedUser) {
        return cachedUser;
      }
    } catch (IllegalStateException ignored) {
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing admin context");
  }

  private void deleteOwnedStores(AuthUser user) {
    Set<UUID> processed = new HashSet<>();

    Optional.ofNullable(user.getOwnedThriftStore())
        .flatMap(store -> thriftStoreRepository.findById(store.getId()))
        .ifPresent(store -> deleteStoreWithAssets(store, processed));

    thriftStoreRepository
        .findByOwnerId(user.getId())
        .forEach(store -> deleteStoreWithAssets(store, processed));
  }

  private void deleteStoreWithAssets(
      com.edufelip.meer.core.store.ThriftStore store, Set<UUID> processed) {
    if (!processed.add(store.getId())) return; // avoid duplicate deletes
    // Remove dependent rows first to satisfy FK constraints
    authUserRepository.deleteFavoritesByStoreId(store.getId());
    storeFeedbackRepository.deleteByThriftStoreId(store.getId());

    if (store.getPhotos() != null) {
      store.getPhotos().forEach(p -> deletePhotoAsset(p.getUrl()));
    }
    thriftStoreRepository.delete(store);
  }

  private void deletePhotoAsset(String url) {
    if (url == null) return;
    if (!deleteLocalIfUploads(url)) {
      try {
        gcsStorageService.deleteByUrl(url);
      } catch (Exception ignored) {
      }
    }
  }

  private boolean deleteLocalIfUploads(String url) {
    if (url == null || !url.startsWith("/uploads/")) return false;
    try {
      Path path = Paths.get("springboot", url.substring(1));
      Files.deleteIfExists(path);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }
}
