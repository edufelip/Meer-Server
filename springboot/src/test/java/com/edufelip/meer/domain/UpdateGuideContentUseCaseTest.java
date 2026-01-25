package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class UpdateGuideContentUseCaseTest {

  @Test
  void updatesFieldsAndDefaults() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    PhotoStoragePort photoStoragePort = Mockito.mock(PhotoStoragePort.class);
    com.edufelip.meer.service.moderation.ModerationPolicyService moderationPolicyService =
        Mockito.mock(com.edufelip.meer.service.moderation.ModerationPolicyService.class);
    UpdateGuideContentUseCase useCase =
        new UpdateGuideContentUseCase(
            repository, storeOwnershipService, photoStoragePort, moderationPolicyService);

    ThriftStore store = new ThriftStore();
    UUID storeId = UUID.randomUUID();
    store.setId(storeId);

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setThriftStore(store);

    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));
    when(repository.save(content)).thenReturn(content);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    String imageUrl = "https://storage.googleapis.com/test-bucket/contents/a";
    when(photoStoragePort.extractFileKey(imageUrl)).thenReturn("contents/a");
    when(photoStoragePort.fetchRequired("contents/a"))
        .thenReturn(new PhotoStoragePort.StoredObject("image/jpeg", 1024L));
    when(photoStoragePort.publicUrl("contents/a"))
        .thenReturn("https://cdn.example.com/contents/a");

    GuideContent updated =
        useCase.execute(user, 10, new UpdateGuideContentUseCase.Command("New", "Desc", imageUrl));

    assertThat(updated.getTitle()).isEqualTo("New");
    assertThat(updated.getDescription()).isEqualTo("Desc");
    assertThat(updated.getImageUrl())
        .isEqualTo("https://cdn.example.com/contents/a");
    assertThat(updated.getCategoryLabel()).isEqualTo("general");
    assertThat(updated.getType()).isEqualTo("article");
    verify(storeOwnershipService).ensureOwnerOrAdminStrict(user, store);
  }

  @Test
  void rejectsInvalidImageUrl() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    PhotoStoragePort photoStoragePort = Mockito.mock(PhotoStoragePort.class);
    com.edufelip.meer.service.moderation.ModerationPolicyService moderationPolicyService =
        Mockito.mock(com.edufelip.meer.service.moderation.ModerationPolicyService.class);
    UpdateGuideContentUseCase useCase =
        new UpdateGuideContentUseCase(
            repository, storeOwnershipService, photoStoragePort, moderationPolicyService);

    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setThriftStore(store);

    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    user, 10, new UpdateGuideContentUseCase.Command("T", "D", "not-a-url")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsExternalImageUrl() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    PhotoStoragePort photoStoragePort = Mockito.mock(PhotoStoragePort.class);
    com.edufelip.meer.service.moderation.ModerationPolicyService moderationPolicyService =
        Mockito.mock(com.edufelip.meer.service.moderation.ModerationPolicyService.class);
    UpdateGuideContentUseCase useCase =
        new UpdateGuideContentUseCase(
            repository, storeOwnershipService, photoStoragePort, moderationPolicyService);

    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setThriftStore(store);

    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));
    when(photoStoragePort.extractFileKey("https://example.com/image.jpg")).thenReturn(null);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    assertThatThrownBy(
            () ->
                useCase.execute(
                    user,
                    10,
                    new UpdateGuideContentUseCase.Command(
                        "T", "D", "https://example.com/image.jpg")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
