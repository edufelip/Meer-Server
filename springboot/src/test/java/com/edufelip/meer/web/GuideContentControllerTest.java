package com.edufelip.meer.web;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.CreateGuideContentCommentUseCase;
import com.edufelip.meer.domain.CreateOwnedGuideContentUseCase;
import com.edufelip.meer.domain.DeleteGuideContentUseCase;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.LikeGuideContentUseCase;
import com.edufelip.meer.domain.RequestGuideContentImageUploadUseCase;
import com.edufelip.meer.domain.UnlikeGuideContentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentCommentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentUseCase;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(GuideContentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, TestClockConfig.class, AuthUserResolver.class})
class GuideContentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetGuideContentUseCase getGuideContentUseCase;
  @MockitoBean private GuideContentRepository guideContentRepository;
  @MockitoBean private GuideContentCommentRepository guideContentCommentRepository;
  @MockitoBean private CreateGuideContentCommentUseCase createGuideContentCommentUseCase;
  @MockitoBean private CreateOwnedGuideContentUseCase createOwnedGuideContentUseCase;
  @MockitoBean private UpdateGuideContentUseCase updateGuideContentUseCase;
  @MockitoBean private DeleteGuideContentUseCase deleteGuideContentUseCase;
  @MockitoBean private LikeGuideContentUseCase likeGuideContentUseCase;
  @MockitoBean private UnlikeGuideContentUseCase unlikeGuideContentUseCase;
  @MockitoBean private UpdateGuideContentCommentUseCase updateGuideContentCommentUseCase;
  @MockitoBean private RequestGuideContentImageUploadUseCase requestGuideContentImageUploadUseCase;
  @MockitoBean private GuideContentEngagementService guideContentEngagementService;
  @MockitoBean private GuideContentModerationService guideContentModerationService;
  @MockitoBean private RateLimitPort rateLimitService;
  @MockitoBean private AuthUserRepository authUserRepository;
  @MockitoBean private TokenProvider tokenProvider;

  @Test
  void requestImageSlotRejectsUnsupportedContentType() throws Exception {
    UUID userId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(requestGuideContentImageUploadUseCase.execute(
            org.mockito.ArgumentMatchers.eq(user),
            org.mockito.ArgumentMatchers.eq(10),
            org.mockito.ArgumentMatchers.eq("image/gif")))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type"));

    mockMvc
        .perform(
            post("/contents/{contentId}/image/upload", 10)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\":\"image/gif\"}"))
        .andExpect(status().isBadRequest());

    verify(requestGuideContentImageUploadUseCase).execute(user, 10, "image/gif");
  }

  @Test
  void requestImageSlotReturnsSlot() throws Exception {
    UUID userId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));

    PhotoStoragePort.UploadSlot slot =
        new PhotoStoragePort.UploadSlot(
            "https://uploads.example.com/slot", "stores/file-key", "image/jpeg");
    when(requestGuideContentImageUploadUseCase.execute(
            org.mockito.ArgumentMatchers.eq(user),
            org.mockito.ArgumentMatchers.eq(10),
            org.mockito.ArgumentMatchers.eq("image/jpeg")))
        .thenReturn(slot);

    mockMvc
        .perform(
            post("/contents/{contentId}/image/upload", 10)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\":\"image/jpeg\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uploadUrl").value("https://uploads.example.com/slot"))
        .andExpect(jsonPath("$.fileKey").value("stores/file-key"))
        .andExpect(jsonPath("$.contentType").value("image/jpeg"));

    verify(requestGuideContentImageUploadUseCase).execute(user, 10, "image/jpeg");
  }

  @Test
  void updateCommentReturnsEditedFlag() throws Exception {
    UUID userId = UUID.randomUUID();
    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    GuideContent content = new GuideContent();
    content.setId(1);

    GuideContentComment comment = new GuideContentComment(user, content, "Old");
    comment.setId(2);

    GuideContentComment updated = new GuideContentComment(user, content, "Updated");
    updated.setId(2);
    updated.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    updated.setEditedAt(Instant.parse("2024-01-02T00:00:00Z"));

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(guideContentRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(content));
    when(guideContentCommentRepository.findById(2)).thenReturn(Optional.of(comment));
    when(rateLimitService.allowCommentEdit(userId.toString())).thenReturn(true);
    when(updateGuideContentCommentUseCase.execute(comment, "Updated", user)).thenReturn(updated);

    mockMvc
        .perform(
            patch("/contents/{id}/comments/{commentId}", 1, 2)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Updated\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.edited").value(true));
  }

  @Test
  void deleteCommentHardDeletes() throws Exception {
    UUID userId = UUID.randomUUID();
    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    GuideContent content = new GuideContent();
    content.setId(1);

    GuideContentComment comment = new GuideContentComment(user, content, "Old");
    comment.setId(2);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(guideContentCommentRepository.findById(2)).thenReturn(Optional.of(comment));

    mockMvc
        .perform(
            delete("/contents/{id}/comments/{commentId}", 1, 2)
                .header("Authorization", "Bearer token"))
        .andExpect(status().isNoContent());

    verify(guideContentModerationService).hardDeleteComment(comment);
  }

  @Test
  void updateCommentForbiddenForStoreOwner() throws Exception {
    UUID ownerId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    ThriftStore store = new ThriftStore();
    store.setId(storeId);

    AuthUser owner = new AuthUser();
    owner.setId(ownerId);
    owner.setEmail("owner@example.com");
    owner.setDisplayName("Owner");
    owner.setPasswordHash("hash");
    owner.setRole(Role.USER);
    owner.setOwnedThriftStore(store);

    AuthUser author = new AuthUser();
    author.setId(authorId);
    author.setEmail("author@example.com");
    author.setDisplayName("Author");
    author.setPasswordHash("hash");
    author.setRole(Role.USER);

    GuideContent content = new GuideContent();
    content.setId(1);
    content.setThriftStore(store);

    GuideContentComment comment = new GuideContentComment(author, content, "Old");
    comment.setId(2);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(ownerId, "owner@example.com", "Owner", Role.USER));
    when(authUserRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    when(guideContentRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(content));
    when(guideContentCommentRepository.findById(2)).thenReturn(Optional.of(comment));

    mockMvc
        .perform(
            patch("/contents/{id}/comments/{commentId}", 1, 2)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Updated\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void createCommentRateLimited() throws Exception {
    UUID userId = UUID.randomUUID();
    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    GuideContent content = new GuideContent();
    content.setId(1);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(guideContentRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(content));
    when(rateLimitService.allowCommentCreate(userId.toString())).thenReturn(false);

    mockMvc
        .perform(
            post("/contents/{id}/comments", 1)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Hi\"}"))
        .andExpect(status().isTooManyRequests());

    verify(createGuideContentCommentUseCase, never())
        .execute(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void updateCommentRateLimited() throws Exception {
    UUID userId = UUID.randomUUID();
    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    GuideContent content = new GuideContent();
    content.setId(1);

    GuideContentComment comment = new GuideContentComment(user, content, "Old");
    comment.setId(2);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(guideContentRepository.findByIdAndDeletedAtIsNull(1)).thenReturn(Optional.of(content));
    when(guideContentCommentRepository.findById(2)).thenReturn(Optional.of(comment));
    when(rateLimitService.allowCommentEdit(userId.toString())).thenReturn(false);

    mockMvc
        .perform(
            patch("/contents/{id}/comments/{commentId}", 1, 2)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Updated\"}"))
        .andExpect(status().isTooManyRequests());

    verify(updateGuideContentCommentUseCase, never())
        .execute(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void likeRateLimited() throws Exception {
    UUID userId = UUID.randomUUID();
    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    GuideContent content = new GuideContent();
    content.setId(1);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many like actions"))
        .when(likeGuideContentUseCase)
        .execute(user, 1);

    mockMvc
        .perform(post("/contents/{id}/likes", 1).header("Authorization", "Bearer token"))
        .andExpect(status().isTooManyRequests());

    verify(likeGuideContentUseCase).execute(user, 1);
  }
}
