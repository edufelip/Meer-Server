package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.domain.PushNotificationException;
import com.edufelip.meer.domain.PushNotificationFailureReason;
import com.edufelip.meer.domain.port.PushNotificationPort;
import com.edufelip.meer.dto.PushBroadcastRequest;
import com.edufelip.meer.dto.PushTestRequest;
import com.edufelip.meer.dto.PushUserNotificationRequest;
import com.edufelip.meer.security.AdminContext;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/dashboard/push")
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class DashboardPushController {

  private static final Logger log = LoggerFactory.getLogger(DashboardPushController.class);

  private final PushNotificationPort pushNotificationService;

  public DashboardPushController(PushNotificationPort pushNotificationService) {
    this.pushNotificationService = pushNotificationService;
  }

  @PostMapping
  public ResponseEntity<Map<String, String>> sendTestPush(
      @RequestHeader("Authorization") String authHeader, @RequestBody @Valid PushTestRequest body) {
    requireAdmin(authHeader);
    String type = normalizeType(body.type());
    try {
      String messageId =
          pushNotificationService.sendTestPush(
              body.token(), body.title(), body.body(), type, body.id());
      return ResponseEntity.ok(Map.of("messageId", messageId));
    } catch (PushNotificationException ex) {
      log.warn(
          "Dashboard test push failed (reason={}, providerCode={})",
          ex.getFailureReason(),
          ex.getProviderErrorCode(),
          ex);
      throw toResponseStatusException(ex);
    }
  }

  @PostMapping("/broadcast")
  public ResponseEntity<Map<String, String>> sendBroadcast(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody @Valid PushBroadcastRequest body) {
    requireAdmin(authHeader);
    PushEnvironment environment = PushEnvironment.parse(body.environment());
    String audience = normalizeAudience(body.audience());
    String type = normalizeType(body.type());
    String topic = buildTopic(audience, environment);
    try {
      String messageId =
          pushNotificationService.sendToTopic(
              topic, body.title(), body.body(), Map.of("type", type, "id", body.id()));
      return ResponseEntity.ok(Map.of("messageId", messageId));
    } catch (PushNotificationException ex) {
      log.warn(
          "Dashboard broadcast push failed (reason={}, providerCode={})",
          ex.getFailureReason(),
          ex.getProviderErrorCode(),
          ex);
      throw toResponseStatusException(ex);
    }
  }

  @PostMapping("/user")
  public ResponseEntity<Map<String, Integer>> sendUserPush(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody @Valid PushUserNotificationRequest body) {
    requireAdmin(authHeader);
    UUID userId = parseUserId(body.userId());
    PushEnvironment environment = PushEnvironment.parse(body.environment());
    String type = normalizeType(body.type());
    int sent =
        pushNotificationService.sendToUser(
            userId, environment, body.title(), body.body(), Map.of("type", type, "id", body.id()));
    return ResponseEntity.ok(Map.of("sent", sent));
  }

  private AuthUser requireAdmin(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
    }
    AuthUser user =
        AdminContext.currentAdmin()
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing admin context"));
    Role role = user.getRole() != null ? user.getRole() : Role.USER;
    if (role != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
    }
    return user;
  }

  private String normalizeType(String type) {
    if (type == null || type.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
    }
    String normalized = type.trim().toLowerCase();
    if (!normalized.equals("guide_content") && !normalized.equals("store")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "type must be guide_content or store");
    }
    return normalized;
  }

  private String normalizeAudience(String audience) {
    if (audience == null || audience.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "audience is required");
    }
    String normalized = audience.trim().toLowerCase();
    if (!normalized.equals("promos") && !normalized.equals("new_stores")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "audience must be promos or new_stores");
    }
    return normalized;
  }

  private String buildTopic(String audience, PushEnvironment environment) {
    String env = environment.name().toLowerCase();
    return audience + "-" + env;
  }

  private UUID parseUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
    }
    try {
      return UUID.fromString(userId.trim());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is invalid");
    }
  }

  private ResponseStatusException toResponseStatusException(PushNotificationException ex) {
    PushNotificationFailureReason reason = ex.getFailureReason();
    String message =
        ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Push send failed";

    if (reason == null) {
      return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    return switch (reason) {
      case TOKEN_NOT_FOUND -> new ResponseStatusException(HttpStatus.NOT_FOUND, message);
      case TOKEN_INVALID -> new ResponseStatusException(HttpStatus.GONE, message);
      case TOKEN_PROJECT_MISMATCH ->
          new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
      case INVALID_ARGUMENT -> new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
      case QUOTA_EXCEEDED -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
      case UNAVAILABLE -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
      case PERMISSION_DENIED, UNAUTHENTICATED ->
          new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
      case THIRD_PARTY_AUTH_ERROR, UNKNOWN ->
          new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    };
  }
}
