package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class UpdateThriftStoreUseCaseTest {

  @Test
  void updateMergesSocialFields() {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);
    UpdateThriftStoreUseCase useCase = new UpdateThriftStoreUseCase(repo, ownershipService);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setSocial(new Social("https://facebook.com/store", "insta", "https://old.com", "wa123"));

    when(repo.findById(storeId)).thenReturn(Optional.of(store));

    UpdateThriftStoreUseCase.SocialUpdate socialUpdate =
        new UpdateThriftStoreUseCase.SocialUpdate(
            null, false, null, false, "https://new.com", true, null, false);
    UpdateThriftStoreUseCase.Command command =
        new UpdateThriftStoreUseCase.Command(
            null, null, null, null, null, null, null, null, null, null, null, null, socialUpdate);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setRole(Role.USER);

    useCase.execute(user, storeId, command);

    assertThat(store.getSocial().getWebsite()).isEqualTo("https://new.com");
    assertThat(store.getSocial().getFacebook()).isEqualTo("https://facebook.com/store");
    assertThat(store.getSocial().getWhatsapp()).isEqualTo("wa123");
    verify(ownershipService).ensureOwnerOrAdmin(user, store);
  }

  @Test
  void updateClearsExplicitNullSocialFields() {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);
    UpdateThriftStoreUseCase useCase = new UpdateThriftStoreUseCase(repo, ownershipService);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setSocial(new Social("https://facebook.com/store", "insta", "https://old.com", "wa123"));

    when(repo.findById(storeId)).thenReturn(Optional.of(store));

    UpdateThriftStoreUseCase.SocialUpdate socialUpdate =
        new UpdateThriftStoreUseCase.SocialUpdate(null, false, null, true, null, true, null, false);
    UpdateThriftStoreUseCase.Command command =
        new UpdateThriftStoreUseCase.Command(
            null, null, null, null, null, null, null, null, null, null, null, null, socialUpdate);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setRole(Role.USER);

    useCase.execute(user, storeId, command);

    assertThat(store.getSocial().getInstagram()).isNull();
    assertThat(store.getSocial().getWebsite()).isNull();
    assertThat(store.getSocial().getFacebook()).isEqualTo("https://facebook.com/store");
    assertThat(store.getSocial().getWhatsapp()).isEqualTo("wa123");
  }

  @Test
  void updateRejectsWebsiteWithoutDotCom() {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);
    UpdateThriftStoreUseCase useCase = new UpdateThriftStoreUseCase(repo, ownershipService);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setSocial(new Social());

    when(repo.findById(storeId)).thenReturn(Optional.of(store));

    UpdateThriftStoreUseCase.SocialUpdate socialUpdate =
        new UpdateThriftStoreUseCase.SocialUpdate(
            null, false, null, false, "https://example.org", true, null, false);
    UpdateThriftStoreUseCase.Command command =
        new UpdateThriftStoreUseCase.Command(
            null, null, null, null, null, null, null, null, null, null, null, null, socialUpdate);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setRole(Role.USER);

    assertThatThrownBy(() -> useCase.execute(user, storeId, command))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
