package com.edufelip.meer.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushPlatform;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.domain.DeletePushTokenUseCase;
import com.edufelip.meer.domain.UpsertPushTokenUseCase;
import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PushTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, TestClockConfig.class, AuthUserResolver.class})
class PushTokenControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TokenProvider tokenProvider;
  @MockitoBean private AuthUserRepository authUserRepository;
  @MockitoBean private UpsertPushTokenUseCase upsertPushTokenUseCase;
  @MockitoBean private DeletePushTokenUseCase deletePushTokenUseCase;

  @Test
  void upsertStoresToken() throws Exception {
    UUID userId = UUID.randomUUID();
    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));

    mockMvc
        .perform(
            post("/push-tokens")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"deviceId\":\"device-1\",\"fcmToken\":\"fcm\",\"platform\":\"android\",\"appVersion\":\"1.0.0\",\"environment\":\"dev\"}"))
        .andExpect(status().isNoContent());

    verify(upsertPushTokenUseCase)
        .execute(
            eq(userId),
            eq("device-1"),
            eq("fcm"),
            eq(PushPlatform.ANDROID),
            eq("1.0.0"),
            eq(PushEnvironment.DEV));
  }

  @Test
  void upsertRejectsInvalidEnvironment() throws Exception {
    UUID userId = UUID.randomUUID();
    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));

    mockMvc
        .perform(
            post("/push-tokens")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"deviceId\":\"device-1\",\"fcmToken\":\"fcm\",\"platform\":\"android\",\"environment\":\"qa\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteRequiresAuthHeader() throws Exception {
    mockMvc.perform(delete("/push-tokens/device-1")).andExpect(status().isUnauthorized());
  }

  @Test
  void deleteParsesEnvironment() throws Exception {
    UUID userId = UUID.randomUUID();
    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));

    mockMvc
        .perform(
            delete("/push-tokens/device-1")
                .queryParam("environment", "prod")
                .header("Authorization", "Bearer token"))
        .andExpect(status().isNoContent());

    verify(deletePushTokenUseCase)
        .execute(eq(userId), eq("device-1"), eq(PushEnvironment.PROD));
  }
}
