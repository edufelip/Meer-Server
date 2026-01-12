package com.edufelip.meer.web;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.dto.NearbyStoreDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NearbyController {

  private final GetThriftStoresUseCase getThriftStoresUseCase;
  private final AuthUserResolver authUserResolver;
  private final StoreFeedbackService storeFeedbackService;

  public NearbyController(
      GetThriftStoresUseCase getThriftStoresUseCase,
      AuthUserResolver authUserResolver,
      StoreFeedbackService storeFeedbackService) {
    this.getThriftStoresUseCase = getThriftStoresUseCase;
    this.authUserResolver = authUserResolver;
    this.storeFeedbackService = storeFeedbackService;
  }

  @GetMapping("/nearby")
  public PageResponse<NearbyStoreDto> nearby(
      @RequestHeader(name = "Authorization", required = false) String authHeader,
      @RequestParam(name = "lat") double lat,
      @RequestParam(name = "lng") double lng,
      @RequestParam(name = "pageIndex", defaultValue = "0") int pageIndex,
      @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
    if (pageIndex < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    var user = authUserResolver.optionalUser(authHeader);
    var page = getThriftStoresUseCase.executeNearby(lat, lng, pageIndex, pageSize);
    List<ThriftStore> stores = page.getContent();

    var summaries =
        storeFeedbackService.getSummaries(stores.stream().map(ThriftStore::getId).toList());

    var items =
        stores.stream()
            .map(
                store -> {
                  var summary = summaries.get(store.getId());
                  Double rating = summary != null ? summary.rating() : null;
                  Integer reviewCount =
                      summary != null && summary.reviewCount() != null
                          ? summary.reviewCount().intValue()
                          : null;
                  return new NearbyStoreDto(
                      store,
                      lat,
                      lng,
                      Mappers.isFavorite(user, store.getId()),
                      rating,
                      reviewCount);
                })
            .toList();

    return new PageResponse<>(items, pageIndex, page.hasNext());
  }
}
