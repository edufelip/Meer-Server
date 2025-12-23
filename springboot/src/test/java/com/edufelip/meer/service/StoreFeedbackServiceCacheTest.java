package com.edufelip.meer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.edufelip.meer.config.CacheConfig;
import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {
      StoreFeedbackService.class,
      StoreFeedbackServiceCacheTest.CacheTestConfig.class,
      TestClockConfig.class
    })
class StoreFeedbackServiceCacheTest {

  @TestConfiguration
  @EnableCaching
  @Import(CacheConfig.class)
  static class CacheTestConfig {}

  @Autowired private StoreFeedbackService storeFeedbackService;
  @Autowired private CacheManager cacheManager;

  @MockitoBean private StoreFeedbackRepository storeFeedbackRepository;

  @Test
  void upsertEvictsStoreRatingsCache() {
    UUID storeId = UUID.randomUUID();
    List<UUID> storeIds = List.of(storeId);

    StoreFeedbackRepository.AggregateView view =
        new StoreFeedbackRepository.AggregateView() {
          @Override
          public UUID getStoreId() {
            return storeId;
          }

          @Override
          public Double getAvgScore() {
            return 4.5;
          }

          @Override
          public Long getCnt() {
            return 2L;
          }
        };

    when(storeFeedbackRepository.aggregateByStoreIds(storeIds)).thenReturn(List.of(view));

    Cache cache = cacheManager.getCache("storeRatings");
    assertThat(cache).isNotNull();
    storeFeedbackService.getSummaries(storeIds);
    assertThat(cache.get(storeIds)).isNotNull();

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setName("Store");
    store.setAddressLine("123 Road");

    when(storeFeedbackRepository.findByUserIdAndThriftStoreId(user.getId(), storeId))
        .thenReturn(Optional.empty());
    when(storeFeedbackRepository.save(org.mockito.Mockito.any(StoreFeedback.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    storeFeedbackService.upsert(user, store, 5, "Great");
    assertThat(cache.get(storeIds)).isNull();
  }

  @Test
  void deleteEvictsStoreRatingsCache() {
    UUID storeId = UUID.randomUUID();
    List<UUID> storeIds = List.of(storeId);

    StoreFeedbackRepository.AggregateView view =
        new StoreFeedbackRepository.AggregateView() {
          @Override
          public UUID getStoreId() {
            return storeId;
          }

          @Override
          public Double getAvgScore() {
            return 3.0;
          }

          @Override
          public Long getCnt() {
            return 1L;
          }
        };

    when(storeFeedbackRepository.aggregateByStoreIds(storeIds)).thenReturn(List.of(view));

    Cache cache = cacheManager.getCache("storeRatings");
    assertThat(cache).isNotNull();
    storeFeedbackService.getSummaries(storeIds);
    assertThat(cache.get(storeIds)).isNotNull();

    UUID userId = UUID.randomUUID();
    StoreFeedback feedback = new StoreFeedback();
    feedback.setId(1);
    when(storeFeedbackRepository.findByUserIdAndThriftStoreId(userId, storeId))
        .thenReturn(Optional.of(feedback));

    storeFeedbackService.delete(userId, storeId);
    assertThat(cache.get(storeIds)).isNull();
  }
}
