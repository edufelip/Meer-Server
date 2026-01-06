package com.edufelip.meer.web;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.core.content.GuideContentLike;
import com.edufelip.meer.domain.CreateGuideContentCommentUseCase;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentCommentUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.ContentCreateRequest;
import com.edufelip.meer.dto.ContentUploadSlotResponse;
import com.edufelip.meer.dto.GuideContentCommentDto;
import com.edufelip.meer.dto.GuideContentCommentRequest;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.PhotoUploadSlot;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.RateLimitService;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GcsStorageService;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import com.edufelip.meer.util.UrlValidatorUtil;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/contents")
public class GuideContentController {

  private final GetGuideContentUseCase getGuideContentUseCase;
  private final GuideContentRepository guideContentRepository;
  private final GuideContentCommentRepository guideContentCommentRepository;
  private final GuideContentLikeRepository guideContentLikeRepository;
  private final CreateGuideContentCommentUseCase createGuideContentCommentUseCase;
  private final UpdateGuideContentCommentUseCase updateGuideContentCommentUseCase;
  private final AuthUserRepository authUserRepository;
  private final TokenProvider tokenProvider;
  private final GcsStorageService gcsStorageService;
  private final ThriftStoreRepository thriftStoreRepository;
  private final GuideContentEngagementService guideContentEngagementService;
  private final GuideContentModerationService guideContentModerationService;
  private final RateLimitService rateLimitService;

  public GuideContentController(
      GetGuideContentUseCase getGuideContentUseCase,
      GuideContentRepository guideContentRepository,
      GuideContentCommentRepository guideContentCommentRepository,
      GuideContentLikeRepository guideContentLikeRepository,
      CreateGuideContentCommentUseCase createGuideContentCommentUseCase,
      UpdateGuideContentCommentUseCase updateGuideContentCommentUseCase,
      AuthUserRepository authUserRepository,
      TokenProvider tokenProvider,
      GcsStorageService gcsStorageService,
      ThriftStoreRepository thriftStoreRepository,
      GuideContentEngagementService guideContentEngagementService,
      GuideContentModerationService guideContentModerationService,
      RateLimitService rateLimitService) {
    this.getGuideContentUseCase = getGuideContentUseCase;
    this.guideContentRepository = guideContentRepository;
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.guideContentLikeRepository = guideContentLikeRepository;
    this.createGuideContentCommentUseCase = createGuideContentCommentUseCase;
    this.updateGuideContentCommentUseCase = updateGuideContentCommentUseCase;
    this.authUserRepository = authUserRepository;
    this.tokenProvider = tokenProvider;
    this.gcsStorageService = gcsStorageService;
    this.thriftStoreRepository = thriftStoreRepository;
    this.guideContentEngagementService = guideContentEngagementService;
    this.guideContentModerationService = guideContentModerationService;
    this.rateLimitService = rateLimitService;
  }

  @GetMapping
  public PageResponse<GuideContentDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(required = false) UUID storeId,
      @RequestHeader(name = "Authorization", required = false) String authHeader) {
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    var user = currentUserOrNull(authHeader);
    Sort.Direction direction =
        "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort s = Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    Pageable pageable = PageRequest.of(page, pageSize, s);
    var slice =
        (q != null && !q.isBlank())
            ? (storeId != null
                ? guideContentRepository.searchSummariesByStoreIdActive(storeId, q, pageable)
                : guideContentRepository.searchSummariesActive(q, pageable))
            : (storeId != null
                ? guideContentRepository.findAllSummariesByStoreIdActive(storeId, pageable)
                : guideContentRepository.findAllSummariesActive(pageable));
    var items = enrichContentDtos(slice.getContent(), user);
    return new PageResponse<>(items, page, slice.hasNext());
  }

  @GetMapping("/{id:\\d+}")
  public GuideContentDto getById(
      @PathVariable Integer id,
      @RequestHeader(name = "Authorization", required = false) String authHeader) {
    var user = currentUserOrNull(authHeader);
    var content = getGuideContentUseCase.execute(id);
    if (content == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found");
    }
    var engagement =
        guideContentEngagementService.getEngagement(
            Collections.singletonList(content.getId()), user != null ? user.getId() : null);
    var summary =
        engagement.getOrDefault(
            content.getId(), new GuideContentEngagementService.EngagementSummary(0L, 0L, false));
    return Mappers.toDto(content, summary.likeCount(), summary.commentCount(), summary.likedByMe());
  }

  @GetMapping("/{id:\\d+}/comments")
  public PageResponse<GuideContentCommentDto> listComments(
      @PathVariable Integer id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestHeader(name = "Authorization", required = false) String authHeader) {
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    currentUserOrNull(authHeader);
    GuideContent content = requireActiveContent(id);
    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
    Pageable pageable = PageRequest.of(page, pageSize, sort);
    var comments =
        guideContentCommentRepository.findByContentIdAndDeletedAtIsNull(content.getId(), pageable);
    var items = comments.getContent().stream().map(Mappers::toDto).toList();
    return new PageResponse<>(items, page, comments.hasNext());
  }

  @PostMapping("/{id:\\d+}/comments")
  public GuideContentCommentDto createComment(
      @PathVariable Integer id,
      @RequestBody GuideContentCommentRequest body,
      @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    if (body == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    GuideContent content = requireActiveContent(id);
    if (!rateLimitService.allowCommentCreate(user.getId().toString())) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many comments");
    }
    GuideContentComment comment =
        createGuideContentCommentUseCase.execute(user, content, body.body());
    return Mappers.toDto(comment);
  }

  @PatchMapping("/{id:\\d+}/comments/{commentId:\\d+}")
  public GuideContentCommentDto updateComment(
      @PathVariable Integer id,
      @PathVariable Integer commentId,
      @RequestBody GuideContentCommentRequest body,
      @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    if (body == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    requireActiveContent(id);
    GuideContentComment comment =
        guideContentCommentRepository
            .findById(commentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    if (comment.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
    }
    if (comment.getContent() == null || !comment.getContent().getId().equals(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
    }
    if (!canEditComment(user, comment)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit comment");
    }
    if (!rateLimitService.allowCommentEdit(user.getId().toString())) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many comment edits");
    }
    GuideContentComment updated =
        updateGuideContentCommentUseCase.execute(comment, body.body(), user);
    return Mappers.toDto(updated);
  }

  @DeleteMapping("/{id:\\d+}/comments/{commentId:\\d+}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable Integer id,
      @PathVariable Integer commentId,
      @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    GuideContentComment comment =
        guideContentCommentRepository
            .findById(commentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    if (comment.getContent() == null || !comment.getContent().getId().equals(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
    }
    if (!canModerateComment(user, comment)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete comment");
    }
    guideContentModerationService.softDeleteComment(comment, user, null);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id:\\d+}/likes")
  public ResponseEntity<Void> likeContent(
      @PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    GuideContent content = requireActiveContent(id);
    if (!rateLimitService.allowLikeAction(user.getId().toString())) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many like actions");
    }
    if (!guideContentLikeRepository.existsByUserIdAndContentId(user.getId(), content.getId())) {
      guideContentLikeRepository.save(new GuideContentLike(user, content));
      guideContentRepository.incrementLikeCount(content.getId());
    }
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id:\\d+}/likes")
  public ResponseEntity<Void> unlikeContent(
      @PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    GuideContent content = requireActiveContent(id);
    if (!rateLimitService.allowLikeAction(user.getId().toString())) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many like actions");
    }
    guideContentLikeRepository
        .findByUserIdAndContentId(user.getId(), content.getId())
        .ifPresent(
            like -> {
              guideContentLikeRepository.delete(like);
              guideContentRepository.decrementLikeCount(content.getId());
            });
    return ResponseEntity.noContent().build();
  }

  @PostMapping
  public GuideContentDto create(
      @RequestBody @Valid ContentCreateRequest body,
      @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    if (body == null || body.title() == null || body.title().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
    }
    if (body.description() == null || body.description().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
    }
    if (body.storeId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId is required");
    }
    var thriftStore =
        thriftStoreRepository
            .findById(body.storeId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    if (!isOwnerOrAdmin(user, thriftStore.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to add content");
    }
    String defaultCategory = "general";
    String defaultType = "article";
    var content =
        new GuideContent(
            null, body.title(), body.description(), defaultCategory, defaultType, "", thriftStore);
    var saved = guideContentRepository.save(content);
    return Mappers.toDto(saved, 0L, 0L, false);
  }

  @PostMapping("/{contentId:\\d+}/image/upload")
  public ContentUploadSlotResponse requestImageSlot(
      @PathVariable Integer contentId,
      @RequestHeader("Authorization") String authHeader,
      @RequestBody(required = false) java.util.Map<String, String> body) {
    var user = currentUser(authHeader);
    var content =
        guideContentRepository
            .findById(contentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    if (content.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found");
    }
    if (content.getThriftStore() == null
        || !isOwnerOrAdmin(user, content.getThriftStore().getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to upload content images");
    }
    String ctype = body != null ? body.get("contentType") : null;
    if (ctype != null
        && !(ctype.equalsIgnoreCase("image/jpeg")
            || ctype.equalsIgnoreCase("image/png")
            || ctype.equalsIgnoreCase("image/webp"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type");
    }
    UUID storeId = content.getThriftStore().getId();
    PhotoUploadSlot slot = gcsStorageService.createUploadSlots(storeId, 1, List.of(ctype)).get(0);
    return new ContentUploadSlotResponse(
        slot.getUploadUrl(), slot.getFileKey(), slot.getContentType());
  }

  @PutMapping("/{id:\\d+}")
  public GuideContentDto update(
      @PathVariable Integer id,
      @RequestBody GuideContent body,
      @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    var content =
        guideContentRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    if (content.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found");
    }
    if (!isOwnerOrAdmin(user, content.getThriftStore().getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to update content");
    }
    if (body.getTitle() != null) content.setTitle(body.getTitle());
    if (body.getDescription() != null) content.setDescription(body.getDescription());
    if (body.getImageUrl() != null) {
      validateHttpUrl(body.getImageUrl(), "imageUrl");
      content.setImageUrl(body.getImageUrl());
    }
    // keep default category/type if still null
    if (content.getCategoryLabel() == null) content.setCategoryLabel("general");
    if (content.getType() == null) content.setType("article");
    guideContentRepository.save(content);
    var engagement =
        guideContentEngagementService.getEngagement(
            Collections.singletonList(content.getId()), user != null ? user.getId() : null);
    var summary =
        engagement.getOrDefault(
            content.getId(), new GuideContentEngagementService.EngagementSummary(0L, 0L, false));
    return Mappers.toDto(content, summary.likeCount(), summary.commentCount(), summary.likedByMe());
  }

  @DeleteMapping("/{id:\\d+}")
  public ResponseEntity<Void> delete(
      @PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
    var user = currentUser(authHeader);
    var content =
        guideContentRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    if (!isOwnerOrAdmin(user, content.getThriftStore().getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to delete content");
    }
    guideContentModerationService.softDeleteContent(content, user, null);
    return ResponseEntity.noContent().build();
  }

  private boolean isOwnerOrAdmin(com.edufelip.meer.core.auth.AuthUser user, UUID storeId) {
    if (user == null || storeId == null) return false;
    if (user.getRole() == com.edufelip.meer.core.auth.Role.ADMIN) return true;
    return user.getOwnedThriftStore() != null && storeId.equals(user.getOwnedThriftStore().getId());
  }

  private com.edufelip.meer.core.auth.AuthUser currentUser(String authHeader) {
    com.edufelip.meer.core.auth.AuthUser user = currentUserOrNull(authHeader);
    if (user == null) throw new InvalidTokenException();
    return user;
  }

  private com.edufelip.meer.core.auth.AuthUser currentUserOrNull(String authHeader) {
    if (authHeader == null) return null;
    if (authHeader.isBlank()) throw new InvalidTokenException();
    if (!authHeader.startsWith("Bearer ")) throw new InvalidTokenException();
    String token = authHeader.substring("Bearer ".length()).trim();
    TokenPayload payload;
    try {
      payload = tokenProvider.parseAccessToken(token);
    } catch (RuntimeException ex) {
      throw new InvalidTokenException();
    }
    return authUserRepository.findById(payload.getUserId()).orElseThrow(InvalidTokenException::new);
  }

  private GuideContent requireActiveContent(Integer contentId) {
    return guideContentRepository
        .findByIdAndDeletedAtIsNull(contentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
  }

  private List<GuideContentDto> enrichContentDtos(
      List<GuideContentDto> base, com.edufelip.meer.core.auth.AuthUser user) {
    if (base == null || base.isEmpty()) return base;
    var ids = base.stream().map(GuideContentDto::id).toList();
    var engagement =
        guideContentEngagementService.getEngagement(ids, user != null ? user.getId() : null);
    return base.stream()
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
  }

  private boolean canModerateComment(
      com.edufelip.meer.core.auth.AuthUser user, GuideContentComment comment) {
    if (user == null) return false;
    if (user.getRole() == com.edufelip.meer.core.auth.Role.ADMIN) return true;
    if (comment.getUser() != null && user.getId().equals(comment.getUser().getId())) return true;
    if (comment.getContent() != null
        && comment.getContent().getThriftStore() != null
        && user.getOwnedThriftStore() != null) {
      return comment
          .getContent()
          .getThriftStore()
          .getId()
          .equals(user.getOwnedThriftStore().getId());
    }
    return false;
  }

  private boolean canEditComment(
      com.edufelip.meer.core.auth.AuthUser user, GuideContentComment comment) {
    if (user == null) return false;
    if (user.getRole() == com.edufelip.meer.core.auth.Role.ADMIN) return true;
    return comment.getUser() != null && user.getId().equals(comment.getUser().getId());
  }

  private void validateHttpUrl(String url, String field) {
    try {
      UrlValidatorUtil.ensureHttpUrl(url, field);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
  }
}
