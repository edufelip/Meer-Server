package com.edufelip.meer.domain;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import java.util.UUID;

public class DeletePushTokenUseCase {
  private final PushTokenRepository pushTokenRepository;

  public DeletePushTokenUseCase(PushTokenRepository pushTokenRepository) {
    this.pushTokenRepository = pushTokenRepository;
  }

  public int execute(UUID userId, String deviceId, PushEnvironment environment) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (deviceId == null || deviceId.isBlank()) {
      throw new IllegalArgumentException("deviceId is required");
    }
    String trimmedDeviceId = deviceId.trim();
    if (environment == null) {
      return pushTokenRepository.deleteByUserAndDevice(userId, trimmedDeviceId);
    }
    return pushTokenRepository.deleteByUserDeviceAndEnvironment(
        userId, trimmedDeviceId, environment);
  }
}
