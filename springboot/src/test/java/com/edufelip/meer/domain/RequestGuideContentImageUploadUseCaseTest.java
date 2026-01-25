package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.testutil.LocalPhotoStorageFake;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RequestGuideContentImageUploadUseCaseTest {

  @Test
  void createsUploadSlotForContentImage() throws Exception {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    LocalPhotoStorageFake storage = new LocalPhotoStorageFake(Files.createTempDirectory("uploads"));

    RequestGuideContentImageUploadUseCase useCase =
        new RequestGuideContentImageUploadUseCase(repository, storeOwnershipService, storage);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setThriftStore(store);

    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    var slot = useCase.execute(user, 10, "image/jpeg");

    assertThat(slot.fileKey()).startsWith("contents/");
    assertThat(slot.contentType()).isEqualTo("image/jpeg");
    verify(storeOwnershipService).ensureOwnerOrAdminStrict(user, store);
  }

  @Test
  void rejectsUnsupportedContentType() throws Exception {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    LocalPhotoStorageFake storage = new LocalPhotoStorageFake(Files.createTempDirectory("uploads"));

    RequestGuideContentImageUploadUseCase useCase =
        new RequestGuideContentImageUploadUseCase(repository, storeOwnershipService, storage);

    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setThriftStore(store);

    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    assertThatThrownBy(() -> useCase.execute(user, 10, "image/gif"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
