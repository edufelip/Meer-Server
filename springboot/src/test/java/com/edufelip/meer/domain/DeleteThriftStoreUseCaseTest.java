package com.edufelip.meer.domain;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeleteThriftStoreUseCaseTest {

  @Test
  void deletesStoreAndCleansRelations() {
    ThriftStoreRepository thriftStoreRepository = Mockito.mock(ThriftStoreRepository.class);
    AuthUserRepository authUserRepository = Mockito.mock(AuthUserRepository.class);
    StoreFeedbackRepository storeFeedbackRepository =
        Mockito.mock(StoreFeedbackRepository.class);
    PhotoStoragePort photoStoragePort = Mockito.mock(PhotoStoragePort.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);

    DeleteThriftStoreUseCase useCase =
        new DeleteThriftStoreUseCase(
            thriftStoreRepository,
            authUserRepository,
            storeFeedbackRepository,
            photoStoragePort,
            ownershipService);

    UUID storeId = UUID.randomUUID();
    AuthUser owner = new AuthUser();
    owner.setId(UUID.randomUUID());
    owner.setRole(Role.USER);

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setOwner(owner);
    owner.setOwnedThriftStore(store);
    ThriftStorePhoto photo = new ThriftStorePhoto(store, "/uploads/stores/photo.jpg", 0);
    store.setPhotos(List.of(photo));

    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));

    AuthUser admin = new AuthUser();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    when(ownershipService.isAdmin(admin)).thenReturn(true);

    useCase.execute(admin, storeId);

    verify(authUserRepository).save(owner);
    verify(authUserRepository).deleteFavoritesByStoreId(storeId);
    verify(storeFeedbackRepository).deleteByThriftStoreId(storeId);
    verify(photoStoragePort).deleteByUrl("/uploads/stores/photo.jpg");
    verify(thriftStoreRepository).delete(store);
  }
}
