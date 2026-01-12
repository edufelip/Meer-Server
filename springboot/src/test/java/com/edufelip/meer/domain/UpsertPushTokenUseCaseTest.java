package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushPlatform;
import com.edufelip.meer.core.push.PushToken;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class UpsertPushTokenUseCaseTest {

  @Test
  void createsNewTokenWithRefreshTimestamp() {
    PushTokenRepository repo = Mockito.mock(PushTokenRepository.class);
    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    UUID userId = UUID.randomUUID();

    when(repo.findByUserIdAndDeviceIdAndEnvironment(userId, "device-1", PushEnvironment.DEV))
        .thenReturn(Optional.empty());

    UpsertPushTokenUseCase useCase = new UpsertPushTokenUseCase(repo, clock);
    useCase.execute(
        userId, "device-1", "token-1", PushPlatform.ANDROID, "1.0.0", PushEnvironment.DEV);

    ArgumentCaptor<PushToken> captor = ArgumentCaptor.forClass(PushToken.class);
    verify(repo).save(captor.capture());

    PushToken saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getDeviceId()).isEqualTo("device-1");
    assertThat(saved.getFcmToken()).isEqualTo("token-1");
    assertThat(saved.getPlatform()).isEqualTo(PushPlatform.ANDROID);
    assertThat(saved.getEnvironment()).isEqualTo(PushEnvironment.DEV);
    assertThat(saved.getAppVersion()).isEqualTo("1.0.0");
    assertThat(saved.getLastSeenAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    assertThat(saved.getLastTokenRefreshAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
  }

  @Test
  void updatesRefreshWhenTokenChanges() {
    PushTokenRepository repo = Mockito.mock(PushTokenRepository.class);
    Clock clock = Clock.fixed(Instant.parse("2025-02-01T10:15:30Z"), ZoneOffset.UTC);
    UUID userId = UUID.randomUUID();

    PushToken existing = new PushToken();
    existing.setUserId(userId);
    existing.setDeviceId("device-2");
    existing.setEnvironment(PushEnvironment.PROD);
    existing.setFcmToken("old-token");
    existing.setLastTokenRefreshAt(Instant.parse("2024-01-01T00:00:00Z"));

    when(repo.findByUserIdAndDeviceIdAndEnvironment(userId, "device-2", PushEnvironment.PROD))
        .thenReturn(Optional.of(existing));

    UpsertPushTokenUseCase useCase = new UpsertPushTokenUseCase(repo, clock);
    useCase.execute(userId, "device-2", "new-token", PushPlatform.IOS, null, PushEnvironment.PROD);

    assertThat(existing.getFcmToken()).isEqualTo("new-token");
    assertThat(existing.getLastTokenRefreshAt()).isEqualTo(Instant.parse("2025-02-01T10:15:30Z"));
    assertThat(existing.getLastSeenAt()).isEqualTo(Instant.parse("2025-02-01T10:15:30Z"));
  }

  @Test
  void keepsRefreshWhenTokenUnchanged() {
    PushTokenRepository repo = Mockito.mock(PushTokenRepository.class);
    Clock clock = Clock.fixed(Instant.parse("2025-03-01T08:00:00Z"), ZoneOffset.UTC);
    UUID userId = UUID.randomUUID();

    PushToken existing = new PushToken();
    existing.setUserId(userId);
    existing.setDeviceId("device-3");
    existing.setEnvironment(PushEnvironment.STAGING);
    existing.setFcmToken("same-token");
    existing.setLastTokenRefreshAt(Instant.parse("2024-06-01T00:00:00Z"));

    when(repo.findByUserIdAndDeviceIdAndEnvironment(userId, "device-3", PushEnvironment.STAGING))
        .thenReturn(Optional.of(existing));

    UpsertPushTokenUseCase useCase = new UpsertPushTokenUseCase(repo, clock);
    useCase.execute(
        userId, "device-3", "same-token", PushPlatform.ANDROID, "2.0.0", PushEnvironment.STAGING);

    assertThat(existing.getLastTokenRefreshAt()).isEqualTo(Instant.parse("2024-06-01T00:00:00Z"));
    assertThat(existing.getLastSeenAt()).isEqualTo(Instant.parse("2025-03-01T08:00:00Z"));
  }
}
