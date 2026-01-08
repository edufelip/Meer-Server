package com.edufelip.meer.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.CreateGuideContentCommentUseCase;
import com.edufelip.meer.domain.CreateOwnedGuideContentUseCase;
import com.edufelip.meer.domain.DeleteGuideContentUseCase;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.GuideContentSummary;
import com.edufelip.meer.domain.LikeGuideContentUseCase;
import com.edufelip.meer.domain.RequestGuideContentImageUploadUseCase;
import com.edufelip.meer.domain.UnlikeGuideContentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentCommentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentUseCase;
import com.edufelip.meer.domain.auth.AppleLoginUseCase;
import com.edufelip.meer.domain.auth.AuthResult;
import com.edufelip.meer.domain.auth.AuthenticatedUser;
import com.edufelip.meer.domain.auth.DashboardLoginUseCase;
import com.edufelip.meer.domain.auth.ForgotPasswordUseCase;
import com.edufelip.meer.domain.auth.GoogleLoginUseCase;
import com.edufelip.meer.domain.auth.LoginUseCase;
import com.edufelip.meer.domain.auth.RefreshTokenUseCase;
import com.edufelip.meer.domain.auth.ResetPasswordUseCase;
import com.edufelip.meer.domain.auth.SignupUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.SupportContactRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.security.SanitizingJacksonModuleConfig;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import com.edufelip.meer.support.SnapshotAssertions;
import com.edufelip.meer.web.AuthController;
import com.edufelip.meer.web.DashboardAuthController;
import com.edufelip.meer.web.GuideContentController;
import com.edufelip.meer.web.SupportController;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {
      AuthController.class,
      DashboardAuthController.class,
      SupportController.class,
      GuideContentController.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({SanitizingJacksonModuleConfig.class, TestClockConfig.class, AuthUserResolver.class})
class PublicApiSnapshotTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LoginUseCase loginUseCase;
  @MockitoBean private SignupUseCase signupUseCase;
  @MockitoBean private GoogleLoginUseCase googleLoginUseCase;
  @MockitoBean private AppleLoginUseCase appleLoginUseCase;
  @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
  @MockitoBean private ForgotPasswordUseCase forgotPasswordUseCase;
  @MockitoBean private ResetPasswordUseCase resetPasswordUseCase;
  @MockitoBean private AuthUserRepository authUserRepository;
  @MockitoBean private TokenProvider tokenProvider;

  @MockitoBean private DashboardLoginUseCase dashboardLoginUseCase;

  @MockitoBean private SupportContactRepository supportContactRepository;
  @MockitoBean private RateLimitPort rateLimitService;

  @MockitoBean private GetGuideContentUseCase getGuideContentUseCase;
  @MockitoBean private GuideContentRepository guideContentRepository;
  @MockitoBean private GuideContentCommentRepository guideContentCommentRepository;
  @MockitoBean private GuideContentLikeRepository guideContentLikeRepository;
  @MockitoBean private CreateGuideContentCommentUseCase createGuideContentCommentUseCase;
  @MockitoBean private UpdateGuideContentCommentUseCase updateGuideContentCommentUseCase;
  @MockitoBean private CreateOwnedGuideContentUseCase createOwnedGuideContentUseCase;
  @MockitoBean private UpdateGuideContentUseCase updateGuideContentUseCase;
  @MockitoBean private DeleteGuideContentUseCase deleteGuideContentUseCase;
  @MockitoBean private LikeGuideContentUseCase likeGuideContentUseCase;
  @MockitoBean private UnlikeGuideContentUseCase unlikeGuideContentUseCase;
  @MockitoBean private GuideContentEngagementService guideContentEngagementService;
  @MockitoBean private GuideContentModerationService guideContentModerationService;
  @MockitoBean private RequestGuideContentImageUploadUseCase requestGuideContentImageUploadUseCase;
  @MockitoBean private ThriftStoreRepository thriftStoreRepository;

  @Test
  void authLoginSnapshot() throws Exception {
    when(loginUseCase.execute(eq("jane@example.com"), eq("secret"))).thenReturn(sampleAuthResult());

    String body =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"jane@example.com\",\"password\":\"secret\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/auth/login.json", body);
  }

  @Test
  void authSignupSnapshot() throws Exception {
    when(signupUseCase.execute(eq("Jane"), eq("jane@example.com"), eq("secret")))
        .thenReturn(sampleAuthResult());

    String body =
        mockMvc
            .perform(
                post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Jane\",\"email\":\"jane@example.com\",\"password\":\"secret\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/auth/signup.json", body);
  }

  @Test
  void authRefreshSnapshot() throws Exception {
    when(refreshTokenUseCase.execute(eq("refresh-token"))).thenReturn(sampleAuthResult());

    String body =
        mockMvc
            .perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"refresh-token\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/auth/refresh.json", body);
  }

  @Test
  void authForgotPasswordSnapshot() throws Exception {
    doNothing().when(forgotPasswordUseCase).execute(eq("jane@example.com"));

    String body =
        mockMvc
            .perform(
                post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"jane@example.com\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/auth/forgot-password.json", body);
  }

  @Test
  void authResetPassword() throws Exception {
    doNothing()
        .when(resetPasswordUseCase)
        .execute(eq("b9c7718b-7f30-4e8f-ae2f-053a221e0f2d"), eq("Password1!"));

    mockMvc
        .perform(
            post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"b9c7718b-7f30-4e8f-ae2f-053a221e0f2d\",\"password\":\"Password1!\"}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void authGoogleSnapshot() throws Exception {
    when(googleLoginUseCase.execute(eq("google-token"), eq("android")))
        .thenReturn(sampleAuthResult());

    String body =
        mockMvc
            .perform(
                post("/auth/google")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"client\":\"android\",\"idToken\":\"google-token\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/auth/google.json", body);
  }

  @Test
  void authAppleSnapshot() throws Exception {
    when(appleLoginUseCase.execute(eq("apple-token"), eq("code"), eq("ios")))
        .thenReturn(sampleAuthResult());

    String body =
        mockMvc
            .perform(
                post("/auth/apple")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"provider\":\"apple\",\"idToken\":\"apple-token\",\"authorizationCode\":\"code\",\"client\":\"ios\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/auth/apple.json", body);
  }

  @Test
  void dashboardLoginSnapshot() throws Exception {
    when(dashboardLoginUseCase.execute(eq("admin@example.com"), eq("secret")))
        .thenReturn(sampleAuthResult());

    String body =
        mockMvc
            .perform(
                post("/dashboard/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"admin@example.com\",\"password\":\"secret\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/dashboard/login.json", body);
  }

  @Test
  void supportContactSnapshot() throws Exception {
    when(supportContactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(rateLimitService.allowSupportContact(any())).thenReturn(true);

    String body =
        mockMvc
            .perform(
                post("/support/contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\",\"message\":\"Help\"}"))
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/support/contact.json", body);
  }

  @Test
  void guideContentListSnapshot() throws Exception {
    UUID storeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
    GuideContentSummary summary =
        new GuideContentSummary(
            1,
            "How to thrift",
            "Desc",
            "https://img",
            storeId,
            "Store",
            "https://cover",
            createdAt);
    var slice = new SliceImpl<>(List.of(summary), PageRequest.of(0, 20), false);
    when(guideContentRepository.findAllSummariesActive(any())).thenReturn(slice);
    when(guideContentEngagementService.getEngagement(any(), any())).thenReturn(java.util.Map.of());

    String body =
        mockMvc
            .perform(get("/contents").param("page", "0").param("pageSize", "20"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/contents/list.json", body);
  }

  @Test
  void guideContentGetSnapshot() throws Exception {
    UUID storeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setName("Store");
    store.setCoverImageUrl("https://cover");

    GuideContent content = new GuideContent();
    content.setId(1);
    content.setTitle("How to thrift");
    content.setDescription("Desc");
    content.setImageUrl("https://img");
    content.setThriftStore(store);
    content.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));

    when(getGuideContentUseCase.execute(eq(1))).thenReturn(content);
    when(guideContentEngagementService.getEngagement(any(), any())).thenReturn(java.util.Map.of());

    String body =
        mockMvc
            .perform(get("/contents/{id}", 1))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    SnapshotAssertions.assertJsonSnapshot("snapshots/contents/get.json", body);
  }

  private AuthResult sampleAuthResult() {
    UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
    AuthenticatedUser user = new AuthenticatedUser(id, "Jane Doe", "jane@example.com", Role.USER);
    return new AuthResult("access-token", "refresh-token", user);
  }
}
