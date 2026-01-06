package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
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
  private final AuthUserRepository authUserRepository;
  private final TokenProvider tokenProvider;
  private final StoreFeedbackService storeFeedbackService;

  public StoreSearchController(
      ThriftStoreRepository thriftStoreRepository,
      AuthUserRepository authUserRepository,
      TokenProvider tokenProvider,
      StoreFeedbackService storeFeedbackService) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.authUserRepository = authUserRepository;
    this.tokenProvider = tokenProvider;
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
    var user = currentUserOrNull(authHeader);
    java.util.Set<com.edufelip.meer.core.store.ThriftStore> favorites =
        user != null ? user.getFavorites() : java.util.Set.of();

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
                  boolean isFav = favorites.stream().anyMatch(f -> f.getId().equals(store.getId()));
                  return Mappers.toDto(store, false, isFav, rating, reviewCount, null);
                })
            .toList();

    return new PageResponse<>(items, page, result.hasNext());
  }

  private com.edufelip.meer.core.auth.AuthUser currentUserOrNull(String authHeader) {
    if (authHeader == null) return null;
    if (!authHeader.startsWith("Bearer ")) throw new InvalidTokenException();
    String token = authHeader.substring("Bearer ".length()).trim();
    TokenPayload payload;
    try {
      payload = tokenProvider.parseAccessToken(token);
    } catch (RuntimeException ex) {
      throw new InvalidTokenException();
    }
    return authUserRepository.findById(payload.getUserId()).orElseThrow(InvalidTokenException::new);
  }
}
