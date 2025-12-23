package com.edufelip.meer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.support.TestFixtures;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StoreFeedbackServiceTest {

  @Test
  void upsertRefreshesCreatedAtForExistingFeedback() {
    StoreFeedbackRepository repository = Mockito.mock(StoreFeedbackRepository.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), java.time.ZoneOffset.UTC);
    StoreFeedbackService service = new StoreFeedbackService(repository, clock);

    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);

    ThriftStore store = new ThriftStore();
    store.setId(storeId);

    StoreFeedback existing = new StoreFeedback(user, store, 3, "old");
    Instant originalCreatedAt = Instant.parse("2000-01-01T00:00:00Z");
    existing.setCreatedAt(originalCreatedAt);
    existing.setUpdatedAt(originalCreatedAt);

    Mockito.when(repository.findByUserIdAndThriftStoreId(userId, storeId))
        .thenReturn(Optional.of(existing));
    Mockito.when(repository.save(Mockito.any(StoreFeedback.class)))
        .thenAnswer(inv -> inv.getArgument(0, StoreFeedback.class));

    StoreFeedback saved = service.upsert(user, store, 5, "new");

    assertNotNull(saved.getCreatedAt());
    assertNotEquals(originalCreatedAt, saved.getCreatedAt());
    assertEquals(TestFixtures.fixedInstant(), saved.getCreatedAt());
  }
}
