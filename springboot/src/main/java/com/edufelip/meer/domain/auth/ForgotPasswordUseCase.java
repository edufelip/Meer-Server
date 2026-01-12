package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.PasswordResetToken;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.PasswordResetTokenRepository;
import com.edufelip.meer.security.PasswordResetProperties;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class ForgotPasswordUseCase {
  private final AuthUserRepository authUserRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordResetNotifier passwordResetNotifier;
  private final PasswordResetProperties passwordResetProperties;
  private final Clock clock;

  public ForgotPasswordUseCase(
      AuthUserRepository authUserRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      PasswordResetNotifier passwordResetNotifier,
      PasswordResetProperties passwordResetProperties,
      Clock clock) {
    this.authUserRepository = authUserRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.passwordResetNotifier = passwordResetNotifier;
    this.passwordResetProperties = passwordResetProperties;
    this.clock = clock;
  }

  @Transactional
  public void execute(String email) {
    if (email == null || email.isBlank()) {
      return;
    }
    var user = authUserRepository.findByEmail(email);
    if (user == null) {
      return; // avoid enumeration
    }
    passwordResetTokenRepository.deleteByUserId(user.getId());
    UUID token = UUID.randomUUID();
    long ttlMinutes = passwordResetProperties.getTtlMinutes();
    Instant expiresAt = Instant.now(clock).plus(Duration.ofMinutes(ttlMinutes));
    passwordResetTokenRepository.save(new PasswordResetToken(token, user, expiresAt));
    String resetLink = passwordResetProperties.buildResetLink(token);
    passwordResetNotifier.sendResetLink(user.getEmail(), resetLink, ttlMinutes);
  }
}
