package com.edufelip.meer.domain;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.service.GuideContentModerationService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeleteGuideContentUseCaseTest {

  @Test
  void deletesContentWhenOwner() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    GuideContentModerationService moderationService =
        Mockito.mock(GuideContentModerationService.class);
    DeleteGuideContentUseCase useCase =
        new DeleteGuideContentUseCase(repository, storeOwnershipService, moderationService);

    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(5);
    content.setThriftStore(store);
    when(repository.findById(5)).thenReturn(Optional.of(content));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    useCase.execute(user, 5);

    verify(storeOwnershipService).ensureOwnerOrAdminStrict(user, store);
    verify(moderationService).softDeleteContent(content, user, null);
  }
}
