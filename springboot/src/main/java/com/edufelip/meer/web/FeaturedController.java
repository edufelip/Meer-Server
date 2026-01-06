package com.edufelip.meer.web;

import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.dto.FeaturedStoreDto;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeaturedController {

  private final GetThriftStoresUseCase getThriftStoresUseCase;
  private final TokenProvider tokenProvider;

  public FeaturedController(
      GetThriftStoresUseCase getThriftStoresUseCase, TokenProvider tokenProvider) {
    this.getThriftStoresUseCase = getThriftStoresUseCase;
    this.tokenProvider = tokenProvider;
  }

  @GetMapping("/featured")
  public List<FeaturedStoreDto> featured(
      @RequestHeader(name = "Authorization", required = false) String authHeader,
      @RequestParam(name = "lat", required = false) Double lat,
      @RequestParam(name = "lng", required = false) Double lng) {
    currentUserOrNull(authHeader); // validate token if provided
    var stores = getThriftStoresUseCase.executeRecentTop10();

    return stores.stream()
        .map(
            s ->
                new FeaturedStoreDto(
                    s.getId(),
                    s.getName(),
                    s.getPhotos() != null && !s.getPhotos().isEmpty()
                        ? s.getPhotos().get(0).getUrl()
                        : s.getCoverImageUrl()))
        .toList();
  }

  private com.edufelip.meer.core.auth.AuthUser currentUserOrNull(String authHeader) {
    if (authHeader == null) return null;
    if (!authHeader.startsWith("Bearer ")) throw new InvalidTokenException();
    String token = authHeader.substring("Bearer ".length()).trim();
    try {
      TokenPayload payload = tokenProvider.parseAccessToken(token);
      return new com.edufelip.meer.core.auth.AuthUser(payload.getUserId(), null, null, null, null);
    } catch (RuntimeException ex) {
      throw new InvalidTokenException();
    }
  }
}
