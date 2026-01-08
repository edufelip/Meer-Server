package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.GetStoreContentsUseCase.ContentItem;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import java.util.UUID;

public class GetStoreDetailsUseCase {

  public record StoreDetails(
      ThriftStore store,
      Double rating,
      Integer reviewCount,
      boolean isFavorite,
      Integer myRating,
      List<ContentItem> contents) {}

  private final GetThriftStoreUseCase getThriftStoreUseCase;
  private final StoreFeedbackService storeFeedbackService;
  private final StoreFeedbackRepository storeFeedbackRepository;
  private final GetStoreContentsUseCase getStoreContentsUseCase;

  public GetStoreDetailsUseCase(
      GetThriftStoreUseCase getThriftStoreUseCase,
      StoreFeedbackService storeFeedbackService,
      StoreFeedbackRepository storeFeedbackRepository,
      GetStoreContentsUseCase getStoreContentsUseCase) {
    this.getThriftStoreUseCase = getThriftStoreUseCase;
    this.storeFeedbackService = storeFeedbackService;
    this.storeFeedbackRepository = storeFeedbackRepository;
    this.getStoreContentsUseCase = getStoreContentsUseCase;
  }

  public StoreDetails execute(UUID storeId, AuthUser user) {
    ThriftStore store = getThriftStoreUseCase.execute(storeId);
    if (store == null) return null;
    var summary = storeFeedbackService.getSummaries(List.of(store.getId())).get(store.getId());
    Double rating = summary != null ? summary.rating() : null;
    Integer reviewCount =
        summary != null && summary.reviewCount() != null ? summary.reviewCount().intValue() : null;
    boolean isFav =
        user != null && user.getFavorites().stream().anyMatch(f -> f.getId().equals(store.getId()));
    Integer myRating =
        user != null
            ? storeFeedbackRepository
                .findByUserIdAndThriftStoreId(user.getId(), store.getId())
                .map(StoreFeedback::getScore)
                .orElse(null)
            : null;
    var contents =
        getStoreContentsUseCase.execute(store.getId(), user != null ? user.getId() : null);
    return new StoreDetails(store, rating, reviewCount, isFav, myRating, contents);
  }
}
