package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.util.StringUtils;

public class AcceptTermsUseCase {
  private final AuthUserRepository authUserRepository;
  private final Clock clock;

  public record Command(String termsVersion, String requiredVersion) {}

  public AcceptTermsUseCase(AuthUserRepository authUserRepository, Clock clock) {
    this.authUserRepository = authUserRepository;
    this.clock = clock;
  }

  public AuthUser execute(AuthUser user, Command command) {
    if (user == null) {
      throw new IllegalArgumentException("User required");
    }
    String normalizedVersion = normalize(command.termsVersion());
    if (!StringUtils.hasText(normalizedVersion)) {
      throw new IllegalArgumentException("terms_version is required");
    }

    String requiredVersion = normalize(command.requiredVersion());
    if (requiredVersion != null && !requiredVersion.equals(normalizedVersion)) {
      throw new TermsVersionMismatchException(requiredVersion, normalizedVersion);
    }

    if (normalizedVersion.equals(user.getTermsVersion())) {
      if (user.getTermsAcceptedAt() == null) {
        user.setTermsAcceptedAt(Instant.now(clock));
        return authUserRepository.save(user);
      }
      return user;
    }

    user.setTermsVersion(normalizedVersion);
    user.setTermsAcceptedAt(Instant.now(clock));
    return authUserRepository.save(user);
  }

  private String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
