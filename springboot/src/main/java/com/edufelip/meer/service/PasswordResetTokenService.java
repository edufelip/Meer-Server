package com.edufelip.meer.service;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.PasswordResetToken;
import com.edufelip.meer.domain.repo.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetTokenService {
  private final PasswordResetTokenRepository repository;
  private final Clock clock;

  public PasswordResetTokenService(PasswordResetTokenRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public void createNewToken(AuthUser user, UUID token, long ttlMinutes) {
    // Invalidate all previous tokens
    repository.deleteByUserId(user.getId());

    // Create new one
    Instant expiresAt = Instant.now(clock).plus(Duration.ofMinutes(ttlMinutes));
    repository.save(new PasswordResetToken(token, user, expiresAt));
  }
}
