package com.edufelip.meer.service;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushToken;
import com.edufelip.meer.domain.PushNotificationException;
import com.edufelip.meer.domain.PushNotificationFailureReason;
import com.edufelip.meer.domain.port.PushNotificationPort;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.google.firebase.ErrorCode;
import com.google.firebase.IncomingHttpResponse;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class PushNotificationService implements PushNotificationPort {
  private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
  private static final String ANDROID_CHANNEL_ID = "default";

  private final FirebaseMessaging firebaseMessaging;
  private final PushTokenRepository pushTokenRepository;

  public PushNotificationService(
      FirebaseMessaging firebaseMessaging, PushTokenRepository pushTokenRepository) {
    this.firebaseMessaging = firebaseMessaging;
    this.pushTokenRepository = pushTokenRepository;
  }

  @Override
  public String sendTestPush(String token, String title, String body, String type, String id)
      throws PushNotificationException {
    UUID tokenId = null;
    String resolvedToken = token;

    // Allow admins to pass the stored PushToken ID (UUID) instead of the raw FCM token.
    try {
      tokenId = UUID.fromString(token.trim());
      PushToken stored = pushTokenRepository.findById(tokenId).orElse(null);
      if (stored == null) {
        throw new PushNotificationException(
            PushNotificationFailureReason.TOKEN_NOT_FOUND,
            "Push token not found: " + tokenId);
      }
      resolvedToken = stored.getFcmToken();
    } catch (IllegalArgumentException ignored) {
      // Not a UUID, assume raw FCM token
    }

    Message message =
        Message.builder()
            .setToken(resolvedToken)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setNotification(
                        AndroidNotification.builder().setChannelId(ANDROID_CHANNEL_ID).build())
                    .build())
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(Aps.builder().setSound("default").build())
                    .build())
            .putData("type", type)
            .putData("id", id)
            .build();
    try {
      return firebaseMessaging.send(message);
    } catch (FirebaseMessagingException ex) {
      logFirebaseFailure("test push", ex);
      PushNotificationFailureReason reason = classifyFailure(ex);
      String providerCode = providerErrorCode(ex);
      if (tokenId != null && reason == PushNotificationFailureReason.TOKEN_INVALID) {
        log.info("Removing invalid push token {} due to provider error {}", tokenId, providerCode);
        pushTokenRepository.deleteById(tokenId);
      }
      throw new PushNotificationException(
          reason,
          providerCode,
          buildFailureMessage("test push", reason, providerCode, ex),
          ex);
    }
  }

  @Override
  public String sendToTopic(String topic, String title, String body, Map<String, String> data)
      throws PushNotificationException {
    Message.Builder builder =
        Message.builder()
            .setTopic(topic)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setNotification(
                        AndroidNotification.builder().setChannelId(ANDROID_CHANNEL_ID).build())
                    .build())
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(Aps.builder().setSound("default").build())
                    .build());
    if (data != null) {
      for (Map.Entry<String, String> entry : data.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          builder.putData(entry.getKey(), entry.getValue());
        }
      }
    }
    try {
      return firebaseMessaging.send(builder.build());
    } catch (FirebaseMessagingException ex) {
      logFirebaseFailure("topic push", ex);
      PushNotificationFailureReason reason = classifyFailure(ex);
      String providerCode = providerErrorCode(ex);
      throw new PushNotificationException(
          reason,
          providerCode,
          buildFailureMessage("topic push", reason, providerCode, ex),
          ex);
    }
  }

  @Override
  public int sendToUser(
      UUID userId,
      PushEnvironment environment,
      String title,
      String body,
      Map<String, String> data) {
    List<PushToken> tokens = pushTokenRepository.findByUserIdAndEnvironment(userId, environment);
    int sent = 0;
    for (PushToken token : tokens) {
      if (sendToStoredToken(token, title, body, data)) {
        sent++;
      }
    }
    return sent;
  }

  public boolean sendToStoredToken(
      PushToken token, String title, String body, Map<String, String> data) {
    log.debug("Sending push to token {} for user {}", token.getId(), token.getUserId());
    Message.Builder builder =
        Message.builder()
            .setToken(token.getFcmToken())
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setNotification(
                        AndroidNotification.builder().setChannelId(ANDROID_CHANNEL_ID).build())
                    .build())
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(Aps.builder().setSound("default").build())
                    .build());
    if (data != null) {
      for (Map.Entry<String, String> entry : data.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          builder.putData(entry.getKey(), entry.getValue());
        }
      }
    }
    Message message = builder.build();
    try {
      firebaseMessaging.send(message);
      return true;
    } catch (FirebaseMessagingException ex) {
      logFirebaseFailure("user push", ex);
      if (shouldDeleteToken(ex)) {
        log.info("Removing invalid push token {} for user {}", token.getId(), token.getUserId());
        pushTokenRepository.deleteById(token.getId());
      } else {
        log.warn("Failed to send push to token {}", token.getId(), ex);
      }
      return false;
    }
  }

  private boolean shouldDeleteToken(FirebaseMessagingException ex) {
    MessagingErrorCode code = ex.getMessagingErrorCode();
    if (code == null) return false;
    String name = code.name();
    return "UNREGISTERED".equals(name) || "NOT_FOUND".equals(name);
  }

  private PushNotificationFailureReason classifyFailure(FirebaseMessagingException ex) {
    if (shouldDeleteToken(ex)) {
      return PushNotificationFailureReason.TOKEN_INVALID;
    }

    MessagingErrorCode messagingCode = ex.getMessagingErrorCode();
    if (messagingCode != null) {
      String name = messagingCode.name();
      if ("SENDER_ID_MISMATCH".equals(name)) {
        return PushNotificationFailureReason.TOKEN_PROJECT_MISMATCH;
      }
      if ("THIRD_PARTY_AUTH_ERROR".equals(name)) {
        return PushNotificationFailureReason.THIRD_PARTY_AUTH_ERROR;
      }
      if ("QUOTA_EXCEEDED".equals(name)) {
        return PushNotificationFailureReason.QUOTA_EXCEEDED;
      }
      if ("UNAVAILABLE".equals(name)) {
        return PushNotificationFailureReason.UNAVAILABLE;
      }
      if ("INVALID_ARGUMENT".equals(name)) {
        return PushNotificationFailureReason.INVALID_ARGUMENT;
      }
    }

    ErrorCode baseCode = ex.getErrorCode();
    if (baseCode != null) {
      String name = baseCode.name();
      if ("PERMISSION_DENIED".equals(name)) {
        return PushNotificationFailureReason.PERMISSION_DENIED;
      }
      if ("UNAUTHENTICATED".equals(name)) {
        return PushNotificationFailureReason.UNAUTHENTICATED;
      }
    }
    return PushNotificationFailureReason.UNKNOWN;
  }

  private String providerErrorCode(FirebaseMessagingException ex) {
    MessagingErrorCode messagingCode = ex.getMessagingErrorCode();
    if (messagingCode != null) {
      return messagingCode.name();
    }
    ErrorCode baseCode = ex.getErrorCode();
    return baseCode != null ? baseCode.name() : null;
  }

  private String buildFailureMessage(
      String action,
      PushNotificationFailureReason reason,
      String providerCode,
      FirebaseMessagingException ex) {
    String hint =
        switch (reason) {
          case TOKEN_INVALID ->
              "FCM token is invalid/unregistered. Ask the user to open the app to re-register.";
          case TOKEN_PROJECT_MISMATCH ->
              "Token belongs to a different Firebase project/environment (sender ID mismatch).";
          case THIRD_PARTY_AUTH_ERROR ->
              "Third-party auth error (commonly APNs configuration missing/invalid for iOS).";
          case PERMISSION_DENIED, UNAUTHENTICATED ->
              "Firebase credentials are missing/invalid or lack messaging permissions.";
          case QUOTA_EXCEEDED -> "Firebase/FCM quota exceeded.";
          case UNAVAILABLE -> "Firebase/FCM is temporarily unavailable.";
          case INVALID_ARGUMENT -> "Invalid request (often a malformed token).";
          case TOKEN_NOT_FOUND, UNKNOWN -> "";
        };

    String details = ex.getMessage() != null ? ex.getMessage().trim() : "";
    String codeSuffix = providerCode != null ? " (" + providerCode + ")" : "";

    StringBuilder message = new StringBuilder("Failed to send " + action + codeSuffix);
    if (!hint.isBlank()) {
      message.append(". ").append(hint);
    }
    if (!details.isBlank()) {
      message.append(" Details: ").append(details);
    }
    return message.toString();
  }

  private void logFirebaseFailure(String action, FirebaseMessagingException ex) {
    Integer statusCode = null;
    String contentSnippet = null;
    IncomingHttpResponse response = ex.getHttpResponse();
    if (response != null) {
      statusCode = response.getStatusCode();
      String content = response.getContent();
      if (content != null && !content.isBlank()) {
        contentSnippet = content.length() > 500 ? content.substring(0, 500) + "..." : content;
      }
    }
    log.warn(
        "Firebase {} failed (errorCode={}, messagingErrorCode={}, httpStatus={}, responseBody={}, message={})",
        action,
        ex.getErrorCode(),
        ex.getMessagingErrorCode(),
        statusCode,
        contentSnippet,
        ex.getMessage());
  }
}
