package com.edufelip.meer.service;

import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;

@Service
public class StoreFeedbackService {

    public record Summary(Double rating, Long reviewCount) {}

    private final StoreFeedbackRepository repository;

    public StoreFeedbackService(StoreFeedbackRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "storeRatings", key = "#storeIds", unless = "#storeIds == null || #storeIds.isEmpty() || #storeIds.size() > 50")
    public Map<java.util.UUID, Summary> getSummaries(List<java.util.UUID> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) return Map.of();
        var aggregates = repository.aggregateByStoreIds(storeIds);
        Map<java.util.UUID, Summary> map = new HashMap<>();
        for (StoreFeedbackRepository.AggregateView view : aggregates) {
            map.put(view.getStoreId(), new Summary(view.getAvgScore(), view.getCnt()));
        }
        return map;
    }

    @CacheEvict(cacheNames = "storeRatings", allEntries = true)
    public com.edufelip.meer.core.store.StoreFeedback upsert(com.edufelip.meer.core.auth.AuthUser user,
                                                            com.edufelip.meer.core.store.ThriftStore store,
                                                            Integer score,
                                                            String body) {
        if (score != null && (score < 1 || score > 5)) {
            throw new IllegalArgumentException("score must be between 1 and 5");
        }
        var existing = repository.findByUserIdAndThriftStoreId(user.getId(), store.getId());
        com.edufelip.meer.core.store.StoreFeedback fb = existing.orElseGet(() -> new com.edufelip.meer.core.store.StoreFeedback(user, store, null, null));
        Instant now = Instant.now();
        fb.setScore(score);
        fb.setBody(body);
        fb.setUser(user);
        fb.setThriftStore(store);
        fb.setCreatedAt(now);
        fb.setUpdatedAt(now);
        return repository.save(fb);
    }

    @CacheEvict(cacheNames = "storeRatings", allEntries = true)
    public java.util.Optional<com.edufelip.meer.core.store.StoreFeedback> find(java.util.UUID userId, java.util.UUID storeId) {
        return repository.findByUserIdAndThriftStoreId(userId, storeId);
    }

    public void delete(java.util.UUID userId, java.util.UUID storeId) {
        repository.findByUserIdAndThriftStoreId(userId, storeId).ifPresent(repository::delete);
    }
}
