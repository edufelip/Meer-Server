package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CreateStoreGuideContentUseCaseTest {

  @Test
  void createsGuideContentWhenOwner() {
    AuthUserRepository authUserRepository = Mockito.mock(AuthUserRepository.class);
    ThriftStoreRepository thriftStoreRepository = Mockito.mock(ThriftStoreRepository.class);
    CreateGuideContentUseCase createGuideContentUseCase =
        Mockito.mock(CreateGuideContentUseCase.class);
    StoreOwnershipService storeOwnershipService =
        new StoreOwnershipService(authUserRepository, thriftStoreRepository);
    CreateStoreGuideContentUseCase useCase =
        new CreateStoreGuideContentUseCase(
            thriftStoreRepository, storeOwnershipService, createGuideContentUseCase);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    store.setOwner(user);

    GuideContent input =
        new GuideContent(1, "Title", "Desc", "cat", "type", "url", null);

    GuideContent saved =
        new GuideContent(1, "Title", "Desc", "cat", "type", "url", store);
    when(createGuideContentUseCase.execute(Mockito.any(GuideContent.class))).thenReturn(saved);

    GuideContent result = useCase.execute(user, storeId, input);

    assertThat(result).isEqualTo(saved);
    ArgumentCaptor<GuideContent> captor = ArgumentCaptor.forClass(GuideContent.class);
    verify(createGuideContentUseCase).execute(captor.capture());
    assertThat(captor.getValue().getThriftStore()).isEqualTo(store);
  }

  @Test
  void rejectsWhenUserDoesNotOwnStore() {
    AuthUserRepository authUserRepository = Mockito.mock(AuthUserRepository.class);
    ThriftStoreRepository thriftStoreRepository = Mockito.mock(ThriftStoreRepository.class);
    CreateGuideContentUseCase createGuideContentUseCase =
        Mockito.mock(CreateGuideContentUseCase.class);
    StoreOwnershipService storeOwnershipService =
        new StoreOwnershipService(authUserRepository, thriftStoreRepository);
    CreateStoreGuideContentUseCase useCase =
        new CreateStoreGuideContentUseCase(
            thriftStoreRepository, storeOwnershipService, createGuideContentUseCase);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    AuthUser owner = new AuthUser();
    owner.setId(UUID.randomUUID());
    store.setOwner(owner);
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    assertThatThrownBy(
            () -> useCase.execute(user, storeId, new GuideContent()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }
}
