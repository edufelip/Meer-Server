package com.edufelip.meer.support;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.core.store.ThriftStore;
import java.time.Instant;
import java.util.UUID;

public final class TestFixtures {

  private TestFixtures() {}

  public static Instant fixedInstant() {
    return TestClockConfig.FIXED_INSTANT;
  }

  public static AuthUser user(String email, String name) {
    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setEmail(email);
    user.setDisplayName(name);
    user.setPasswordHash("hash");
    return user;
  }

  public static ThriftStore store(String name) {
    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());
    store.setName(name);
    store.setAddressLine("123 Road");
    return store;
  }

  public static StoreFeedback feedback(
      AuthUser user, ThriftStore store, Integer score, String body) {
    StoreFeedback feedback = new StoreFeedback(user, store, score, body);
    feedback.setCreatedAt(fixedInstant());
    feedback.setUpdatedAt(fixedInstant());
    return feedback;
  }
}
