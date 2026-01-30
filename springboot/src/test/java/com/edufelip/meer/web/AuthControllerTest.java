package com.edufelip.meer.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.auth.AppleLoginUseCase;
import com.edufelip.meer.domain.auth.ForgotPasswordUseCase;
import com.edufelip.meer.domain.auth.GoogleLoginUseCase;
import com.edufelip.meer.domain.auth.LoginUseCase;
import com.edufelip.meer.domain.auth.RefreshTokenUseCase;
import com.edufelip.meer.domain.auth.ResetPasswordUseCase;
import com.edufelip.meer.domain.auth.SignupUseCase;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.support.TestFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LoginUseCase loginUseCase;
  @MockitoBean private SignupUseCase signupUseCase;
  @MockitoBean private GoogleLoginUseCase googleLoginUseCase;
  @MockitoBean private AppleLoginUseCase appleLoginUseCase;
  @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
  @MockitoBean private ForgotPasswordUseCase forgotPasswordUseCase;
  @MockitoBean private ResetPasswordUseCase resetPasswordUseCase;
  @MockitoBean private com.edufelip.meer.security.AuthUserResolver authUserResolver;
  @MockitoBean private ProfileAssembler profileAssembler;

  @Test
  void meIncludesTermsFields() throws Exception {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    ProfileDto profile =
        new ProfileDto(
            user.getId(),
            user.getDisplayName(),
            user.getEmail(),
            "2025-01",
            Instant.parse("2025-01-12T14:55:00Z"),
            "2026-01",
            "https://www.guiabrecho.com.br/terms-eula",
            user.getPhotoUrl(),
            user.getBio(),
            "USER",
            user.isNotifyNewStores(),
            user.isNotifyPromos(),
            null,
            user.getCreatedAt());

    when(authUserResolver.requireUser(eq("Bearer token"))).thenReturn(user);
    when(profileAssembler.toProfileDto(eq(user), eq(true))).thenReturn(profile);

    mockMvc
        .perform(get("/auth/me").header("Authorization", "Bearer token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.terms_version").value("2025-01"))
        .andExpect(jsonPath("$.user.terms_accepted_at").value("2025-01-12T14:55:00Z"))
        .andExpect(jsonPath("$.user.terms_required_version").value("2026-01"))
        .andExpect(
            jsonPath("$.user.terms_url").value("https://www.guiabrecho.com.br/terms-eula"));
  }
}
