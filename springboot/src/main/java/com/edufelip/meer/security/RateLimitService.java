package com.edufelip.meer.security;

import com.edufelip.meer.domain.port.RateLimitPort;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService implements RateLimitPort {

  private static final int COMMENT_CREATE_LIMIT = 10;
  private static final int COMMENT_EDIT_LIMIT = 20;
  private static final int LIKE_LIMIT = 60;
  private static final int SUPPORT_CONTACT_LIMIT = 3;

  private final Cache<String, AtomicInteger> perMinuteCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).maximumSize(100_000).build();

  private final Cache<String, AtomicInteger> perHourCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(50_000).build();

  @Override
  public boolean allowCommentCreate(String userKey) {
    return incrementWithinLimit(perMinuteCache, "comment:create:" + userKey, COMMENT_CREATE_LIMIT);
  }

  @Override
  public boolean allowCommentEdit(String userKey) {
    return incrementWithinLimit(perMinuteCache, "comment:edit:" + userKey, COMMENT_EDIT_LIMIT);
  }

  @Override
  public boolean allowLikeAction(String userKey) {
    return incrementWithinLimit(perMinuteCache, "content:like:" + userKey, LIKE_LIMIT);
  }

  @Override
  public boolean allowSupportContact(String clientKey) {
    return incrementWithinLimit(
        perHourCache, "support:contact:" + clientKey, SUPPORT_CONTACT_LIMIT);
  }

  private boolean incrementWithinLimit(Cache<String, AtomicInteger> cache, String key, int limit) {
    AtomicInteger counter = cache.get(key, k -> new AtomicInteger(0));
    int count = counter.incrementAndGet();
    return count <= limit;
  }
}
