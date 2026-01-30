package com.edufelip.meer.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.support.TestFixtures;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
class AcceptTermsUseCaseTest {

  @Autowired private AuthUserRepository authUserRepository;
  @Autowired private AcceptTermsUseCase useCase;
  @Autowired private EntityManager entityManager;

  @Test
  void acceptsWhenVersionMatchesRequired() {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    authUserRepository.save(user);

    useCase.execute(user, new AcceptTermsUseCase.Command("2025-01", "2025-01"));

    entityManager.flush();
    entityManager.clear();

    AuthUser updated = authUserRepository.findById(user.getId()).orElseThrow();

    assertThat(updated.getTermsVersion()).isEqualTo("2025-01");
    assertThat(updated.getTermsAcceptedAt()).isEqualTo(TestClockConfig.FIXED_INSTANT);
  }

  @Test
  void rejectsWhenVersionDoesNotMatchRequired() {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    authUserRepository.save(user);

    assertThatThrownBy(
            () -> useCase.execute(user, new AcceptTermsUseCase.Command("2024-12", "2025-01")))
        .isInstanceOf(TermsVersionMismatchException.class)
        .hasMessageContaining("terms_version does not match required version");
  }

  @Test
  void doesNotUpdateAcceptedAtWhenVersionAlreadyAccepted() {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    Instant acceptedAt = TestClockConfig.FIXED_INSTANT.minusSeconds(3600);
    user.setTermsVersion("2025-01");
    user.setTermsAcceptedAt(acceptedAt);
    authUserRepository.save(user);

    useCase.execute(user, new AcceptTermsUseCase.Command("2025-01", "2025-01"));

    entityManager.flush();
    entityManager.clear();

    AuthUser updated = authUserRepository.findById(user.getId()).orElseThrow();

    assertThat(updated.getTermsAcceptedAt()).isEqualTo(acceptedAt);
  }

  @Test
  void fillsAcceptedAtWhenVersionMatchesAndTimestampMissing() {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    user.setTermsVersion("2025-01");
    user.setTermsAcceptedAt(null);
    authUserRepository.save(user);

    useCase.execute(user, new AcceptTermsUseCase.Command("2025-01", "2025-01"));

    entityManager.flush();
    entityManager.clear();

    AuthUser updated = authUserRepository.findById(user.getId()).orElseThrow();

    assertThat(updated.getTermsAcceptedAt()).isEqualTo(TestClockConfig.FIXED_INSTANT);
  }

  @Test
  void rejectsBlankVersion() {
    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    authUserRepository.save(user);

    assertThatThrownBy(() -> useCase.execute(user, new AcceptTermsUseCase.Command(" ", "2025-01")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("terms_version is required");
  }
}
