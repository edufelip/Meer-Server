package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
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
    UpdateGuideContentUseCase useCase =
        new UpdateGuideContentUseCase(repository, storeOwnershipService);

    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setThriftStore(store);

    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));
    when(repository.save(content)).thenReturn(content);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    GuideContent updated =
        useCase.execute(
            user,
            10,
            new UpdateGuideContentUseCase.Command(
                "New", "Desc", "https://example.com/image.jpg"));

    assertThat(updated.getTitle()).isEqualTo("New");
    assertThat(updated.getDescription()).isEqualTo("Desc");
    assertThat(updated.getImageUrl()).isEqualTo("https://example.com/image.jpg");
    assertThat(updated.getCategoryLabel()).isEqualTo("general");
    assertThat(updated.getType()).isEqualTo("article");
    verify(storeOwnershipService).ensureOwnerOrAdminStrict(user, store);
  }

  @Test
  void rejectsInvalidImageUrl() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    StoreOwnershipService storeOwnershipService = Mockito.mock(StoreOwnershipService.class);
    UpdateGuideContentUseCase useCase =
        new UpdateGuideContentUseCase(repository, storeOwnershipService);

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
                    user,
                    10,
                    new UpdateGuideContentUseCase.Command("T", "D", "not-a-url")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
