package com.edufelip.meer.service;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushToken;
import com.edufelip.meer.domain.PushNotificationException;
import com.edufelip.meer.domain.port.PushNotificationPort;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
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
    Message message =
        Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setNotification(
                        AndroidNotification.builder().setChannelId(ANDROID_CHANNEL_ID).build())
                    .build())
            .putData("type", type)
            .putData("id", id)
            .build();
    try {
      return firebaseMessaging.send(message);
    } catch (FirebaseMessagingException ex) {
      throw new PushNotificationException("Failed to send test push", ex);
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
      throw new PushNotificationException("Failed to send topic push", ex);
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
}
