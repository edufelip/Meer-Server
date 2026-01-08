package com.edufelip.meer.web;

import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.dto.FeaturedStoreDto;
import com.edufelip.meer.security.AuthUserResolver;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeaturedController {

  private final GetThriftStoresUseCase getThriftStoresUseCase;
  private final AuthUserResolver authUserResolver;

  public FeaturedController(
      GetThriftStoresUseCase getThriftStoresUseCase, AuthUserResolver authUserResolver) {
    this.getThriftStoresUseCase = getThriftStoresUseCase;
    this.authUserResolver = authUserResolver;
  }

  @GetMapping("/featured")
  public List<FeaturedStoreDto> featured(
      @RequestHeader(name = "Authorization", required = false) String authHeader,
      @RequestParam(name = "lat", required = false) Double lat,
      @RequestParam(name = "lng", required = false) Double lng) {
    authUserResolver.optionalPayload(authHeader); // validate token if provided
    var stores = getThriftStoresUseCase.executeRecentTop10();

    return stores.stream()
        .map(FeaturedStoreDto::new)
        .toList();
  }

}
