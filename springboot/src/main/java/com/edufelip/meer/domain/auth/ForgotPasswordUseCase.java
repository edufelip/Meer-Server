package com.edufelip.meer.domain.auth;

import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.PasswordResetProperties;
import com.edufelip.meer.service.PasswordResetTokenService;
import java.util.UUID;

public class ForgotPasswordUseCase {
  private final AuthUserRepository authUserRepository;
  private final PasswordResetTokenService passwordResetTokenService;
  private final PasswordResetNotifier passwordResetNotifier;
  private final PasswordResetProperties passwordResetProperties;

  public ForgotPasswordUseCase(
      AuthUserRepository authUserRepository,
      PasswordResetTokenService passwordResetTokenService,
      PasswordResetNotifier passwordResetNotifier,
      PasswordResetProperties passwordResetProperties) {
    this.authUserRepository = authUserRepository;
    this.passwordResetTokenService = passwordResetTokenService;
    this.passwordResetNotifier = passwordResetNotifier;
    this.passwordResetProperties = passwordResetProperties;
  }

  public void execute(String email) {
    if (email == null || email.isBlank()) {
      return;
    }

    String normalizedEmail = email.trim();
    if (normalizedEmail.isEmpty()) {
      return;
    }

    var user = authUserRepository.findByEmail(normalizedEmail);
    if (user == null) {
      return; // avoid enumeration
    }

    UUID token = UUID.randomUUID();
    long ttlMinutes = passwordResetProperties.getTtlMinutes();

    // Step 1: Commit to DB (atomic transaction)
    passwordResetTokenService.createNewToken(user, token, ttlMinutes);

    // Step 2: Send email (only if Step 1 succeeded)
    String resetLink = passwordResetProperties.buildResetLink(token);
    passwordResetNotifier.sendResetLink(user.getEmail(), resetLink, ttlMinutes);
  }
}