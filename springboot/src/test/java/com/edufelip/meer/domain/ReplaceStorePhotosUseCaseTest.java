package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.testutil.LocalPhotoStorageFake;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReplaceStorePhotosUseCaseTest {

  @Test
  void replacesPhotosWithLocalStorageFake() throws Exception {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    StoreOwnershipService ownershipService = Mockito.mock(StoreOwnershipService.class);
    LocalPhotoStorageFake storageFake =
        new LocalPhotoStorageFake(Files.createTempDirectory("photos"));
    com.edufelip.meer.service.moderation.ModerationPolicyService moderationPolicyService =
        Mockito.mock(com.edufelip.meer.service.moderation.ModerationPolicyService.class);

    ReplaceStorePhotosUseCase useCase =
        new ReplaceStorePhotosUseCase(repo, ownershipService, storageFake, moderationPolicyService);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    ThriftStorePhoto existing = new ThriftStorePhoto(store, "/uploads/stores/old.jpg", 0);
    existing.setId(1);
    store.setPhotos(new ArrayList<>(List.of(existing)));

    when(repo.findById(storeId)).thenReturn(Optional.of(store));

    String newFileKey = "stores/" + storeId + "/new.jpg";
    storageFake.storeObject(newFileKey, "image/jpeg", new byte[] {1, 2, 3});

    List<ReplaceStorePhotosUseCase.PhotoItem> items =
        List.of(
            new ReplaceStorePhotosUseCase.PhotoItem(1, null, 0),
            new ReplaceStorePhotosUseCase.PhotoItem(null, newFileKey, 1));

    ReplaceStorePhotosUseCase.Command command = new ReplaceStorePhotosUseCase.Command(items, null);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    ThriftStore result = useCase.execute(user, storeId, command);

    assertThat(result.getPhotos()).hasSize(2);
    assertThat(result.getPhotos().get(0).getUrl()).isEqualTo("/uploads/stores/old.jpg");
    assertThat(result.getPhotos().get(1).getUrl()).isEqualTo("/uploads/" + newFileKey);
    assertThat(result.getCoverImageUrl()).isEqualTo("/uploads/stores/old.jpg");
    verify(ownershipService).ensureOwnerOrAdmin(user, store);
  }
}
