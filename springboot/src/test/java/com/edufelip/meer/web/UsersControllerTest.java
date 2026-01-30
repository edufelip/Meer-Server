package com.edufelip.meer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.auth.AcceptTermsUseCase;
import com.edufelip.meer.domain.auth.TermsVersionMismatchException;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.service.TermsPolicy;
import com.edufelip.meer.support.TestFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UsersController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestClockConfig.class)
class UsersControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AcceptTermsUseCase acceptTermsUseCase;
  @MockitoBean private TermsPolicy termsPolicy;
  @MockitoBean private ProfileAssembler profileAssembler;
  @MockitoBean private com.edufelip.meer.security.AuthUserResolver authUserResolver;

  @Test
  void acceptTermsReturnsProfilePayload() throws Exception {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    AuthUser updated = TestFixtures.user("jane@example.com", "Jane");
    updated.setId(user.getId());
    updated.setTermsVersion("2025-01");
    updated.setTermsAcceptedAt(Instant.parse("2024-01-02T00:00:00Z"));

    when(authUserResolver.requireUser(eq("Bearer token"))).thenReturn(user);
    when(termsPolicy.requiredVersionOrNull()).thenReturn("2025-01");
    when(acceptTermsUseCase.execute(eq(user), any())).thenReturn(updated);
    ProfileDto profile =
        new ProfileDto(
            updated.getId(),
            updated.getDisplayName(),
            updated.getEmail(),
            updated.getTermsVersion(),
            updated.getTermsAcceptedAt(),
            "2025-01",
            "https://www.guiabrecho.com.br/terms-eula",
            updated.getPhotoUrl(),
            updated.getBio(),
            "USER",
            updated.isNotifyNewStores(),
            updated.isNotifyPromos(),
            null,
            updated.getCreatedAt());
    when(profileAssembler.toProfileDto(eq(updated), eq(true))).thenReturn(profile);

    mockMvc
        .perform(
            post("/users/me/accept-terms")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"terms_version\":\"2025-01\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.terms_version").value("2025-01"))
        .andExpect(jsonPath("$.user.terms_required_version").value("2025-01"))
        .andExpect(
            jsonPath("$.user.terms_url").value("https://www.guiabrecho.com.br/terms-eula"))
        .andExpect(jsonPath("$.user.terms_accepted_at").value("2024-01-02T00:00:00Z"))
        .andExpect(jsonPath("$.user.email").value("jane@example.com"));

    ArgumentCaptor<AcceptTermsUseCase.Command> commandCaptor =
        ArgumentCaptor.forClass(AcceptTermsUseCase.Command.class);
    verify(acceptTermsUseCase).execute(eq(user), commandCaptor.capture());
    assertThat(commandCaptor.getValue().termsVersion()).isEqualTo("2025-01");
    assertThat(commandCaptor.getValue().requiredVersion()).isEqualTo("2025-01");
  }

  @Test
  void acceptTermsRejectsMismatchedVersion() throws Exception {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    when(authUserResolver.requireUser(eq("Bearer token"))).thenReturn(user);
    when(termsPolicy.requiredVersionOrNull()).thenReturn("2026-01");
    when(acceptTermsUseCase.execute(eq(user), any()))
        .thenThrow(new TermsVersionMismatchException("2026-01", "2025-01"));

    mockMvc
        .perform(
            post("/users/me/accept-terms")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"terms_version\":\"2025-01\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("required=")));
  }

  @Test
  void acceptTermsRejectsBlankVersion() throws Exception {
    mockMvc
        .perform(
            post("/users/me/accept-terms")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"terms_version\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("terms_version is required"));
  }
}
