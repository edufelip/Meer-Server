package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.testutil.LocalPhotoStorageFake;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RequestStorePhotoUploadsUseCaseTest {

  @Test
  void createsUploadSlotsWhenWithinLimits() throws Exception {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);
    LocalPhotoStorageFake storageFake =
        new LocalPhotoStorageFake(Files.createTempDirectory("uploads"));

    RequestStorePhotoUploadsUseCase useCase =
        new RequestStorePhotoUploadsUseCase(repo, ownershipService, storageFake);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setPhotos(List.of(new ThriftStorePhoto(store, "/uploads/old.jpg", 0)));
    when(repo.findById(storeId)).thenReturn(Optional.of(store));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    var slots = useCase.execute(user, storeId, 2, List.of("image/jpeg", "image/webp"));

    assertThat(slots).hasSize(2);
    assertThat(slots.get(0).fileKey()).startsWith("stores/" + storeId + "/photos/");
    verify(ownershipService).ensureOwnerOrAdmin(user, store);
  }

  @Test
  void rejectsUnsupportedContentTypes() throws Exception {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);
    LocalPhotoStorageFake storageFake =
        new LocalPhotoStorageFake(Files.createTempDirectory("uploads"));

    RequestStorePhotoUploadsUseCase useCase =
        new RequestStorePhotoUploadsUseCase(repo, ownershipService, storageFake);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    when(repo.findById(storeId)).thenReturn(Optional.of(store));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    assertThatThrownBy(() -> useCase.execute(user, storeId, 1, List.of("image/png")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
