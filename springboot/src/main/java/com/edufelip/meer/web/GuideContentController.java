package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.domain.CreateGuideContentCommentUseCase;
import com.edufelip.meer.domain.CreateOwnedGuideContentUseCase;
import com.edufelip.meer.domain.DeleteGuideContentUseCase;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.LikeGuideContentUseCase;
import com.edufelip.meer.domain.RequestGuideContentImageUploadUseCase;
import com.edufelip.meer.domain.UnlikeGuideContentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentCommentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentUseCase;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.dto.ContentCreateRequest;
import com.edufelip.meer.dto.ContentUploadSlotResponse;
import com.edufelip.meer.dto.GuideContentCommentDto;
import com.edufelip.meer.dto.GuideContentCommentRequest;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
  private final CreateGuideContentCommentUseCase createGuideContentCommentUseCase;
  private final UpdateGuideContentCommentUseCase updateGuideContentCommentUseCase;
  private final RequestGuideContentImageUploadUseCase requestGuideContentImageUploadUseCase;
  private final CreateOwnedGuideContentUseCase createOwnedGuideContentUseCase;
  private final UpdateGuideContentUseCase updateGuideContentUseCase;
  private final DeleteGuideContentUseCase deleteGuideContentUseCase;
  private final LikeGuideContentUseCase likeGuideContentUseCase;
  private final UnlikeGuideContentUseCase unlikeGuideContentUseCase;
  private final AuthUserResolver authUserResolver;
  private final GuideContentEngagementService guideContentEngagementService;
  private final GuideContentModerationService guideContentModerationService;
  private final RateLimitPort rateLimitService;

  public GuideContentController(
      GetGuideContentUseCase getGuideContentUseCase,
      GuideContentRepository guideContentRepository,
      GuideContentCommentRepository guideContentCommentRepository,
      CreateGuideContentCommentUseCase createGuideContentCommentUseCase,
      UpdateGuideContentCommentUseCase updateGuideContentCommentUseCase,
      RequestGuideContentImageUploadUseCase requestGuideContentImageUploadUseCase,
      CreateOwnedGuideContentUseCase createOwnedGuideContentUseCase,
      UpdateGuideContentUseCase updateGuideContentUseCase,
      DeleteGuideContentUseCase deleteGuideContentUseCase,
      LikeGuideContentUseCase likeGuideContentUseCase,
      UnlikeGuideContentUseCase unlikeGuideContentUseCase,
      AuthUserResolver authUserResolver,
      GuideContentEngagementService guideContentEngagementService,
      GuideContentModerationService guideContentModerationService,
      RateLimitPort rateLimitService) {
    this.getGuideContentUseCase = getGuideContentUseCase;
    this.guideContentRepository = guideContentRepository;
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.createGuideContentCommentUseCase = createGuideContentCommentUseCase;
    this.updateGuideContentCommentUseCase = updateGuideContentCommentUseCase;
    this.requestGuideContentImageUploadUseCase = requestGuideContentImageUploadUseCase;
    this.createOwnedGuideContentUseCase = createOwnedGuideContentUseCase;
    this.updateGuideContentUseCase = updateGuideContentUseCase;
    this.deleteGuideContentUseCase = deleteGuideContentUseCase;
    this.likeGuideContentUseCase = likeGuideContentUseCase;
    this.unlikeGuideContentUseCase = unlikeGuideContentUseCase;
    this.authUserResolver = authUserResolver;
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
    var user = authUserResolver.optionalUser(authHeader);
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
    var summaries = slice.getContent().stream().map(Mappers::toDto).toList();
    var items = enrichContentDtos(summaries, user);
    return new PageResponse<>(items, page, slice.hasNext());
  }

  @GetMapping("/{id:\\d+}")
  public GuideContentDto getById(
      @PathVariable Integer id,
      @RequestHeader(name = "Authorization", required = false) String authHeader) {
    var user = authUserResolver.optionalUser(authHeader);
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
    authUserResolver.optionalUser(authHeader);
    GuideContent content = requireActiveContent(id);
    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
    Pageable pageable = PageRequest.of(page, pageSize, sort);
    var comments = guideContentCommentRepository.findByContentId(content.getId(), pageable);
    var items = comments.getContent().stream().map(Mappers::toDto).toList();
    return new PageResponse<>(items, page, comments.hasNext());
  }

  @PostMapping("/{id:\\d+}/comments")
  public GuideContentCommentDto createComment(
      @PathVariable Integer id,
      @RequestBody GuideContentCommentRequest body,
      @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
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
    var user = authUserResolver.requireUser(authHeader);
    if (body == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    requireActiveContent(id);
    GuideContentComment comment =
        guideContentCommentRepository
            .findById(commentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
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
    var user = authUserResolver.requireUser(authHeader);
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
    guideContentModerationService.hardDeleteComment(comment);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id:\\d+}/likes")
  public ResponseEntity<Void> likeContent(
      @PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    likeGuideContentUseCase.execute(user, id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id:\\d+}/likes")
  public ResponseEntity<Void> unlikeContent(
      @PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    unlikeGuideContentUseCase.execute(user, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping
  public GuideContentDto create(
      @RequestBody @Valid ContentCreateRequest body,
      @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    if (body == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    var command =
        new CreateOwnedGuideContentUseCase.Command(
            body.title(), body.description(), body.storeId());
    var saved = createOwnedGuideContentUseCase.execute(user, command);
    return Mappers.toDto(saved, 0L, 0L, false);
  }

  @PostMapping("/{contentId:\\d+}/image/upload")
  public ContentUploadSlotResponse requestImageSlot(
      @PathVariable Integer contentId,
      @RequestHeader("Authorization") String authHeader,
      @RequestBody(required = false) Map<String, String> body) {
    var user = authUserResolver.requireUser(authHeader);
    String ctype = body != null ? body.get("contentType") : null;
    var slot = requestGuideContentImageUploadUseCase.execute(user, contentId, ctype);
    return new ContentUploadSlotResponse(slot.uploadUrl(), slot.fileKey(), slot.contentType());
  }

  @PutMapping("/{id:\\d+}")
  public GuideContentDto update(
      @PathVariable Integer id,
      @RequestBody GuideContent body,
      @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    if (body == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    var command =
        new UpdateGuideContentUseCase.Command(
            body.getTitle(), body.getDescription(), body.getImageUrl());
    var content = updateGuideContentUseCase.execute(user, id, command);
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
    var user = authUserResolver.requireUser(authHeader);
    deleteGuideContentUseCase.execute(user, id);
    return ResponseEntity.noContent().build();
  }

  private GuideContent requireActiveContent(Integer contentId) {
    return guideContentRepository
        .findByIdAndDeletedAtIsNull(contentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
  }

  private List<GuideContentDto> enrichContentDtos(List<GuideContentDto> base, AuthUser user) {
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

  private boolean canModerateComment(AuthUser user, GuideContentComment comment) {
    if (user == null) return false;
    if (user.getRole() == Role.ADMIN) return true;
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

  private boolean canEditComment(AuthUser user, GuideContentComment comment) {
    if (user == null) return false;
    if (user.getRole() == Role.ADMIN) return true;
    return comment.getUser() != null && user.getId().equals(comment.getUser().getId());
  }
}
