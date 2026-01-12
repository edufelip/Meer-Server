package com.edufelip.meer.domain;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushPlatform;
import com.edufelip.meer.core.push.PushToken;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpsertPushTokenUseCase {
  private static final Logger log = LoggerFactory.getLogger(UpsertPushTokenUseCase.class);
  private final PushTokenRepository pushTokenRepository;
  private final Clock clock;

  public UpsertPushTokenUseCase(PushTokenRepository pushTokenRepository, Clock clock) {
    this.pushTokenRepository = pushTokenRepository;
    this.clock = clock;
  }

  public void execute(
      UUID userId,
      String deviceId,
      String fcmToken,
      PushPlatform platform,
      String appVersion,
      PushEnvironment environment) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (deviceId == null || deviceId.isBlank()) {
      throw new IllegalArgumentException("deviceId is required");
    }
    if (fcmToken == null || fcmToken.isBlank()) {
      throw new IllegalArgumentException("fcmToken is required");
    }
    if (platform == null) {
      throw new IllegalArgumentException("platform is required");
    }
    if (environment == null) {
      throw new IllegalArgumentException("environment is required");
    }

    String trimmedDeviceId = deviceId.trim();
    String trimmedToken = fcmToken.trim();
    String normalizedAppVersion =
        appVersion != null && !appVersion.isBlank() ? appVersion.trim() : null;

    Instant now = Instant.now(clock);
    PushToken token =
        pushTokenRepository
            .findByUserIdAndDeviceIdAndEnvironment(userId, trimmedDeviceId, environment)
            .orElseGet(PushToken::new);

    boolean isNew = token.getId() == null;
    if (isNew) {
      token.setUserId(userId);
      token.setDeviceId(trimmedDeviceId);
      token.setEnvironment(environment);
    }

    boolean tokenChanged = !trimmedToken.equals(token.getFcmToken());
    if (tokenChanged) {
      token.setFcmToken(trimmedToken);
      token.setLastTokenRefreshAt(now);
      log.info(
          "FCM token refreshed for user {} device {} env {}", userId, trimmedDeviceId, environment);
    } else if (token.getLastTokenRefreshAt() == null) {
      token.setLastTokenRefreshAt(now);
    }

    token.setPlatform(platform);
    token.setAppVersion(normalizedAppVersion);
    token.setLastSeenAt(now);

    pushTokenRepository.save(token);
  }
}
