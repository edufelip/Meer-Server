package com.edufelip.meer.domain;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeleteThriftStoreUseCaseTest {

  @Test
  void deletesStoreAndCleansRelations() {
    ThriftStoreRepository thriftStoreRepository = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);
    StoreDeletionService storeDeletionService = Mockito.mock(StoreDeletionService.class);

    DeleteThriftStoreUseCase useCase =
        new DeleteThriftStoreUseCase(thriftStoreRepository, ownershipService, storeDeletionService);

    UUID storeId = UUID.randomUUID();
    AuthUser owner = new AuthUser();
    owner.setId(UUID.randomUUID());
    owner.setRole(Role.USER);

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setOwner(owner);
    owner.setOwnedThriftStore(store);
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));

    AuthUser admin = new AuthUser();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    when(ownershipService.isAdmin(admin)).thenReturn(true);

    useCase.execute(admin, storeId);

    verify(storeDeletionService)
        .deleteStoreWithAssets(Mockito.eq(store), Mockito.anySet(), Mockito.eq("STORE_DELETE"));
  }
}
