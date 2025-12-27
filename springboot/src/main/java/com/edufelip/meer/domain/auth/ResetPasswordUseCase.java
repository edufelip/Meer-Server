package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.PasswordResetToken;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ResetPasswordUseCase {
  private static final Pattern PASSWORD_PATTERN =
      Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$");
  private static final String PASSWORD_MESSAGE =
      "Password must be at least 6 characters and include an uppercase letter, a number, and a special character.";

  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final AuthUserRepository authUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public ResetPasswordUseCase(
      PasswordResetTokenRepository passwordResetTokenRepository,
      AuthUserRepository authUserRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.authUserRepository = authUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Transactional
  public void execute(String tokenValue, String password) {
    UUID tokenId = parseToken(tokenValue);
    PasswordResetToken resetToken =
        passwordResetTokenRepository
            .findForUpdateByToken(tokenId)
            .orElseThrow(() -> new ResetPasswordException("Reset token is invalid."));

    Instant now = Instant.now(clock);
    if (resetToken.getUsedAt() != null) {
      throw new ResetPasswordException("Reset token has already been used.");
    }
    if (resetToken.getExpiresAt() == null || !resetToken.getExpiresAt().isAfter(now)) {
      throw new ResetPasswordException("Reset token has expired.");
    }

    validatePassword(password);

    var user = resetToken.getUser();
    user.setPasswordHash(passwordEncoder.encode(password));
    resetToken.setUsedAt(now);
    authUserRepository.save(user);
    passwordResetTokenRepository.save(resetToken);
  }

  private UUID parseToken(String tokenValue) {
    if (tokenValue == null || tokenValue.isBlank()) {
      throw new ResetPasswordException("Reset token is required.");
    }
    try {
      return UUID.fromString(tokenValue.trim());
    } catch (IllegalArgumentException ex) {
      throw new ResetPasswordException("Reset token is invalid.");
    }
  }

  private void validatePassword(String password) {
    if (password == null || password.isBlank()) {
      throw new ResetPasswordException("Password is required.");
    }
    if (!PASSWORD_PATTERN.matcher(password).matches()) {
      throw new ResetPasswordException(PASSWORD_MESSAGE);
    }
  }
}
