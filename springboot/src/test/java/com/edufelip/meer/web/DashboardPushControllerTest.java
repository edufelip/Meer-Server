package com.edufelip.meer.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.domain.PushNotificationException;
import com.edufelip.meer.domain.port.PushNotificationPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardPushController.class, properties = "firebase.enabled=true")
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, TestClockConfig.class})
class DashboardPushControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PushNotificationPort pushNotificationService;

  @Test
  void sendTestPushReturnsMessageId() throws Exception {
    AuthUser admin = adminUser();
    when(pushNotificationService.sendTestPush(
            eq("token-1"),
            eq("Hello"),
            eq("Body"),
            eq("guide_content"),
            eq("123")))
        .thenReturn("msg-1");

    mockMvc
        .perform(
            post("/dashboard/push")
                .requestAttr("adminUser", admin)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"token-1\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"guide_content\",\"id\":\"123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messageId").value("msg-1"));
  }

  @Test
  void sendTestPushRejectsInvalidType() throws Exception {
    AuthUser admin = adminUser();

    mockMvc
        .perform(
            post("/dashboard/push")
                .requestAttr("adminUser", admin)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"token-1\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"unknown\",\"id\":\"123\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(pushNotificationService);
  }

  @Test
  void sendTestPushRequiresAdminContext() throws Exception {
    mockMvc
        .perform(
            post("/dashboard/push")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"token-1\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"guide_content\",\"id\":\"123\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void sendTestPushHandlesFirebaseFailure() throws Exception {
    AuthUser admin = adminUser();
    PushNotificationException ex = new PushNotificationException("fail");
    when(pushNotificationService.sendTestPush(
            eq("token-1"),
            eq("Hello"),
            eq("Body"),
            eq("guide_content"),
            eq("123")))
        .thenThrow(ex);

    mockMvc
        .perform(
            post("/dashboard/push")
                .requestAttr("adminUser", admin)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\"token-1\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"guide_content\",\"id\":\"123\"}"))
        .andExpect(status().isBadGateway());
  }

  @Test
  void sendUserPushReturnsCount() throws Exception {
    AuthUser admin = adminUser();
    UUID userId = UUID.randomUUID();
    when(pushNotificationService.sendToUser(
            eq(userId),
            eq(PushEnvironment.DEV),
            eq("Hello"),
            eq("Body"),
            eq(java.util.Map.of("type", "guide_content", "id", "123"))))
        .thenReturn(2);

    mockMvc
        .perform(
            post("/dashboard/push/user")
                .requestAttr("adminUser", admin)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"userId\":\""
                        + userId
                        + "\",\"environment\":\"dev\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"guide_content\",\"id\":\"123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sent").value(2));
  }

  @Test
  void sendUserPushRejectsInvalidUserId() throws Exception {
    AuthUser admin = adminUser();

    mockMvc
        .perform(
            post("/dashboard/push/user")
                .requestAttr("adminUser", admin)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"userId\":\"not-a-uuid\",\"environment\":\"dev\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"guide_content\",\"id\":\"123\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sendBroadcastReturnsMessageId() throws Exception {
    AuthUser admin = adminUser();
    when(pushNotificationService.sendToTopic(
            eq("promos-dev"),
            eq("Hello"),
            eq("Body"),
            eq(java.util.Map.of("type", "guide_content", "id", "123"))))
        .thenReturn("msg-2");

    mockMvc
        .perform(
            post("/dashboard/push/broadcast")
                .requestAttr("adminUser", admin)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"environment\":\"dev\",\"audience\":\"promos\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"guide_content\",\"id\":\"123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messageId").value("msg-2"));
  }

  @Test
  void sendBroadcastRejectsInvalidAudience() throws Exception {
    AuthUser admin = adminUser();

    mockMvc
        .perform(
            post("/dashboard/push/broadcast")
                .requestAttr("adminUser", admin)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"environment\":\"dev\",\"audience\":\"all\",\"title\":\"Hello\",\"body\":\"Body\",\"type\":\"guide_content\",\"id\":\"123\"}"))
        .andExpect(status().isBadRequest());
  }

  private AuthUser adminUser() {
    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setEmail("admin@example.com");
    user.setDisplayName("Admin");
    user.setPasswordHash("hash");
    user.setRole(Role.ADMIN);
    return user;
  }
}
