package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.StoreFeedbackService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StoreSearchController {

  private final ThriftStoreRepository thriftStoreRepository;
  private final AuthUserResolver authUserResolver;
  private final StoreFeedbackService storeFeedbackService;

  public StoreSearchController(
      ThriftStoreRepository thriftStoreRepository,
      AuthUserResolver authUserResolver,
      StoreFeedbackService storeFeedbackService) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.authUserResolver = authUserResolver;
    this.storeFeedbackService = storeFeedbackService;
  }

  @GetMapping("/stores/search")
  public PageResponse<ThriftStoreDto> search(
      @RequestParam(name = "q") String q,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
      @RequestHeader(name = "Authorization", required = false) String authHeader) {
    if (q == null || q.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q is required");
    if (page < 1 || pageSize < 1 || pageSize > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    var user = authUserResolver.optionalUser(authHeader);

    var pageable = PageRequest.of(page - 1, pageSize);
    var result = thriftStoreRepository.search(q, pageable);

    var ids =
        result.getContent().stream().map(com.edufelip.meer.core.store.ThriftStore::getId).toList();
    var summaries = storeFeedbackService.getSummaries(ids);

    var items =
        result.getContent().stream()
            .map(
                store -> {
                  var summary = summaries.get(store.getId());
                  Double rating = summary != null ? summary.rating() : null;
                  Integer reviewCount =
                      summary != null && summary.reviewCount() != null
                          ? summary.reviewCount().intValue()
                          : null;
                  return Mappers.toDto(
                      store,
                      false,
                      Mappers.isFavorite(user, store.getId()),
                      rating,
                      reviewCount,
                      null);
                })
            .toList();

    return new PageResponse<>(items, page, result.hasNext());
  }
}
