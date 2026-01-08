package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GetStoreDetailsUseCaseTest {

  @Test
  void returnsDetailsWithRatingsAndContents() {
    GetThriftStoreUseCase getThriftStoreUseCase = Mockito.mock(GetThriftStoreUseCase.class);
    StoreFeedbackService storeFeedbackService = Mockito.mock(StoreFeedbackService.class);
    StoreFeedbackRepository storeFeedbackRepository = Mockito.mock(StoreFeedbackRepository.class);
    GetStoreContentsUseCase getStoreContentsUseCase = Mockito.mock(GetStoreContentsUseCase.class);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);

    when(getThriftStoreUseCase.execute(storeId)).thenReturn(store);
    when(storeFeedbackService.getSummaries(List.of(storeId)))
        .thenReturn(Map.of(storeId, new StoreFeedbackService.Summary(4.0, 3L)));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setFavorites(Set.of(store));

    StoreFeedback feedback = new StoreFeedback();
    feedback.setScore(5);
    when(storeFeedbackRepository.findByUserIdAndThriftStoreId(user.getId(), storeId))
        .thenReturn(Optional.of(feedback));

    var content =
        new com.edufelip.meer.core.content.GuideContent(
            1, "Title", "Desc", "cat", "type", "url", store);
    var contentItem =
        new GetStoreContentsUseCase.ContentItem(
            content, new GuideContentEngagementService.EngagementSummary(1L, 2L, true));
    when(getStoreContentsUseCase.execute(storeId, user.getId()))
        .thenReturn(List.of(contentItem));

    GetStoreDetailsUseCase useCase =
        new GetStoreDetailsUseCase(
            getThriftStoreUseCase,
            storeFeedbackService,
            storeFeedbackRepository,
            getStoreContentsUseCase);

    var details = useCase.execute(storeId, user);

    assertThat(details).isNotNull();
    assertThat(details.store()).isEqualTo(store);
    assertThat(details.rating()).isEqualTo(4.0);
    assertThat(details.reviewCount()).isEqualTo(3);
    assertThat(details.isFavorite()).isTrue();
    assertThat(details.myRating()).isEqualTo(5);
    assertThat(details.contents()).hasSize(1);
    assertThat(details.contents().get(0).content()).isEqualTo(content);
    assertThat(details.contents().get(0).engagement().likedByMe()).isTrue();

    verify(getStoreContentsUseCase).execute(storeId, user.getId());
  }
}
