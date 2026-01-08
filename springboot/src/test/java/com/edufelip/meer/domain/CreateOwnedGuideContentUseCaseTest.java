package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CreateOwnedGuideContentUseCaseTest {

  @Test
  void createsGuideContentWithDefaults() {
    ThriftStoreRepository thriftStoreRepository = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    CreateGuideContentUseCase createGuideContentUseCase = Mockito.mock(CreateGuideContentUseCase.class);

    CreateOwnedGuideContentUseCase useCase =
        new CreateOwnedGuideContentUseCase(
            thriftStoreRepository, storeOwnershipService, createGuideContentUseCase);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));

    GuideContent saved = new GuideContent();
    when(createGuideContentUseCase.execute(Mockito.any(GuideContent.class))).thenReturn(saved);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    GuideContent result =
        useCase.execute(user, new CreateOwnedGuideContentUseCase.Command("Title", "Desc", storeId));

    assertThat(result).isEqualTo(saved);
    ArgumentCaptor<GuideContent> captor = ArgumentCaptor.forClass(GuideContent.class);
    verify(createGuideContentUseCase).execute(captor.capture());
    GuideContent created = captor.getValue();
    assertThat(created.getThriftStore()).isEqualTo(store);
    assertThat(created.getCategoryLabel()).isEqualTo("general");
    assertThat(created.getType()).isEqualTo("article");
    assertThat(created.getImageUrl()).isEqualTo("");
    verify(storeOwnershipService).ensureOwnerOrAdminStrict(user, store);
  }

  @Test
  void rejectsMissingStoreId() {
    CreateOwnedGuideContentUseCase useCase =
        new CreateOwnedGuideContentUseCase(
            Mockito.mock(ThriftStoreRepository.class),
            Mockito.mock(StoreOwnershipService.class),
            Mockito.mock(CreateGuideContentUseCase.class));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    assertThatThrownBy(
            () -> useCase.execute(user, new CreateOwnedGuideContentUseCase.Command("T", "D", null)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
