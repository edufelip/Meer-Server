package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GetStoreListingsUseCaseTest {

  @Test
  void listsPagedStoresWithSummariesAndDistance() {
    GetThriftStoresUseCase getThriftStoresUseCase = Mockito.mock(GetThriftStoresUseCase.class);
    ThriftStoreRepository thriftStoreRepository = Mockito.mock(ThriftStoreRepository.class);
    StoreFeedbackService storeFeedbackService = Mockito.mock(StoreFeedbackService.class);
    CategoryRepository categoryRepository = Mockito.mock(CategoryRepository.class);

    UUID storeId = UUID.randomUUID();
    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setLatitude(10.0);
    store.setLongitude(10.0);

    when(getThriftStoresUseCase.executePaged(1, 20))
        .thenReturn(new PageImpl<>(List.of(store)));
    when(storeFeedbackService.getSummaries(List.of(storeId)))
        .thenReturn(Map.of(storeId, new StoreFeedbackService.Summary(4.5, 2L)));

    AuthUser user = new AuthUser();
    user.setFavorites(Set.of(store));

    GetStoreListingsUseCase useCase =
        new GetStoreListingsUseCase(
            getThriftStoresUseCase, thriftStoreRepository, storeFeedbackService, categoryRepository);

    var result =
        useCase.execute(
            new GetStoreListingsUseCase.ListingQuery(null, null, null, 10.0, 10.0, 2, 20), user);

    assertThat(result.items()).hasSize(1);
    var item = result.items().get(0);
    assertThat(item.isFavorite()).isTrue();
    assertThat(item.rating()).isEqualTo(4.5);
    assertThat(item.reviewCount()).isEqualTo(2);
    assertThat(item.distanceMeters()).isEqualTo(0.0);
    verify(getThriftStoresUseCase).executePaged(1, 20);
    verify(storeFeedbackService).getSummaries(List.of(storeId));
  }

  @Test
  void rejectsInvalidPagination() {
    GetStoreListingsUseCase useCase =
        new GetStoreListingsUseCase(
            Mockito.mock(GetThriftStoresUseCase.class),
            Mockito.mock(ThriftStoreRepository.class),
            Mockito.mock(StoreFeedbackService.class),
            Mockito.mock(CategoryRepository.class));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new GetStoreListingsUseCase.ListingQuery(
                        null, null, null, null, null, 0, 10),
                    null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsNearbyWithoutCoordinates() {
    GetStoreListingsUseCase useCase =
        new GetStoreListingsUseCase(
            Mockito.mock(GetThriftStoresUseCase.class),
            Mockito.mock(ThriftStoreRepository.class),
            Mockito.mock(StoreFeedbackService.class),
            Mockito.mock(CategoryRepository.class));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new GetStoreListingsUseCase.ListingQuery(
                        "nearby", null, null, null, 10.0, 1, 10),
                    null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsMissingCategory() {
    CategoryRepository categoryRepository = Mockito.mock(CategoryRepository.class);
    when(categoryRepository.existsById("missing")).thenReturn(false);

    GetStoreListingsUseCase useCase =
        new GetStoreListingsUseCase(
            Mockito.mock(GetThriftStoresUseCase.class),
            Mockito.mock(ThriftStoreRepository.class),
            Mockito.mock(StoreFeedbackService.class),
            categoryRepository);

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new GetStoreListingsUseCase.ListingQuery(
                        null, "missing", null, null, null, 1, 10),
                    null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }
}
