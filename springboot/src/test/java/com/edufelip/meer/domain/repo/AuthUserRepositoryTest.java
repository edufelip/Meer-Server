package com.edufelip.meer.domain.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class AuthUserRepositoryTest {

  private static final char LIKE_ESCAPE = '!';

  @Autowired private AuthUserRepository authUserRepository;

  @Test
  void findNonAdminUsersExcludesAdmins() {
    AuthUser admin = buildUser("admin@example.com", "Admin", Role.ADMIN);
    AuthUser user = buildUser("user@example.com", "User", Role.USER);
    authUserRepository.save(admin);
    authUserRepository.save(user);

    var page = authUserRepository.findNonAdminUsers(Role.ADMIN, PageRequest.of(0, 10));

    assertThat(page.getContent())
        .extracting(AuthUser::getEmail)
        .contains(user.getEmail())
        .doesNotContain(admin.getEmail());
  }

  @Test
  void searchNonAdminUsersHonorsEscapedWildcards() {
    AuthUser adminMatch = buildUser("admin2@example.com", "ja%ne_!", Role.ADMIN);
    AuthUser userMatch = buildUser("user2@example.com", "ja%ne_!", Role.USER);
    authUserRepository.save(adminMatch);
    authUserRepository.save(userMatch);

    String escaped = escapeLikeTerm("ja%ne_!");
    var page = authUserRepository.searchNonAdminUsers(escaped, Role.ADMIN, PageRequest.of(0, 10));

    assertThat(page.getContent())
        .extracting(AuthUser::getEmail)
        .contains(userMatch.getEmail())
        .doesNotContain(adminMatch.getEmail());
  }

  private AuthUser buildUser(String email, String displayName, Role role) {
    AuthUser user = new AuthUser();
    user.setEmail(email);
    user.setDisplayName(displayName);
    user.setPasswordHash("hash");
    user.setRole(role);
    return user;
  }

  private String escapeLikeTerm(String term) {
    return term
        .replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE) + LIKE_ESCAPE)
        .replace("%", LIKE_ESCAPE + "%")
        .replace("_", LIKE_ESCAPE + "_");
  }
}
