package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GetStoreListingsUseCase {

  public record ListingQuery(
      String type, String categoryId, String q, Double lat, Double lng, int page, int pageSize) {}

  public record StoreListItem(
      ThriftStore store,
      Double rating,
      Integer reviewCount,
      boolean isFavorite,
      Double distanceMeters) {}

  public record StoreListResult(List<StoreListItem> items, boolean hasNext) {}

  private final GetThriftStoresUseCase getThriftStoresUseCase;
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreFeedbackService storeFeedbackService;
  private final CategoryRepository categoryRepository;

  public GetStoreListingsUseCase(
      GetThriftStoresUseCase getThriftStoresUseCase,
      ThriftStoreRepository thriftStoreRepository,
      StoreFeedbackService storeFeedbackService,
      CategoryRepository categoryRepository) {
    this.getThriftStoresUseCase = getThriftStoresUseCase;
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeFeedbackService = storeFeedbackService;
    this.categoryRepository = categoryRepository;
  }

  public StoreListResult execute(ListingQuery query, AuthUser user) {
    validateQuery(query);
    var pageable = PageRequest.of(query.page() - 1, query.pageSize());
    var result =
        "nearby".equalsIgnoreCase(query.type())
            ? getThriftStoresUseCase.executeNearby(
                query.lat(), query.lng(), query.page() - 1, query.pageSize())
            : null;
    if (result == null && query.q() != null && !query.q().isBlank()) {
      result = thriftStoreRepository.searchRanked(query.q().trim(), pageable);
    }
    if (result == null && query.categoryId() != null) {
      result = thriftStoreRepository.findByCategoryId(query.categoryId(), pageable);
    }
    if (result == null) {
      result = getThriftStoresUseCase.executePaged(query.page() - 1, query.pageSize());
    }

    var storesPage = result.getContent();
    var ids = storesPage.stream().map(ThriftStore::getId).toList();
    var summaries = storeFeedbackService.getSummaries(ids);
    Set<ThriftStore> favorites = user != null ? user.getFavorites() : Set.of();

    var items =
        storesPage.stream()
            .map(
                store -> {
                  var summary = summaries.get(store.getId());
                  Double rating = summary != null ? summary.rating() : null;
                  Integer reviewCount =
                      summary != null && summary.reviewCount() != null
                          ? summary.reviewCount().intValue()
                          : null;
                  boolean isFav =
                      user != null
                          && favorites.stream().anyMatch(f -> f.getId().equals(store.getId()));
                  Double distanceMeters =
                      (query.lat() != null
                              && query.lng() != null
                              && store.getLatitude() != null
                              && store.getLongitude() != null)
                          ? distanceKm(
                                  query.lat(),
                                  query.lng(),
                                  store.getLatitude(),
                                  store.getLongitude())
                              * 1000
                          : null;
                  return new StoreListItem(store, rating, reviewCount, isFav, distanceMeters);
                })
            .toList();

    return new StoreListResult(items, result.hasNext());
  }

  private void validateQuery(ListingQuery query) {
    if (query.page() < 1 || query.pageSize() < 1 || query.pageSize() > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    if ("nearby".equalsIgnoreCase(query.type())) {
      if (query.lat() == null || query.lng() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "lat and lng are required for nearby search");
      }
      return;
    }
    if ((query.q() == null || query.q().isBlank())
        && query.categoryId() != null
        && !categoryRepository.existsById(query.categoryId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
    }
  }

  private double distanceKm(double lat1, double lon1, Double lat2, Double lon2) {
    if (lat2 == null || lon2 == null) return Double.MAX_VALUE;
    double R = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}
