package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.FavoriteStoreDto;
import com.edufelip.meer.dto.FavoritesVersionDto;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/favorites")
public class FavoritesController {

  private final AuthUserResolver authUserResolver;
  private final AuthUserRepository authUserRepository;
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreFeedbackService storeFeedbackService;

  public FavoritesController(
      AuthUserResolver authUserResolver,
      AuthUserRepository authUserRepository,
      ThriftStoreRepository thriftStoreRepository,
      StoreFeedbackService storeFeedbackService) {
    this.authUserResolver = authUserResolver;
    this.authUserRepository = authUserRepository;
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeFeedbackService = storeFeedbackService;
  }

  @GetMapping
  public List<FavoriteStoreDto> listFavorites(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(name = "lat", required = false) Double lat,
      @RequestParam(name = "lng", required = false) Double lng) {
    AuthUser user = authUserResolver.requireUser(authHeader);
    var ids = user.getFavorites().stream().map(f -> f.getId()).toList();
    var summaries = storeFeedbackService.getSummaries(ids);
    return user.getFavorites().stream()
        .map(
            store -> {
              var summary = summaries.get(store.getId());
              Double rating = summary != null ? summary.rating() : null;
              Integer reviewCount =
                  summary != null && summary.reviewCount() != null
                      ? summary.reviewCount().intValue()
                      : null;
              return new FavoriteStoreDto(store, lat, lng, true);
            })
        .toList();
  }

  @PostMapping("/{storeId}")
  public ResponseEntity<Void> addFavorite(
      @RequestHeader("Authorization") String authHeader, @PathVariable java.util.UUID storeId) {
    AuthUser user = authUserResolver.requireUser(authHeader);
    var store =
        thriftStoreRepository
            .findById(storeId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    user.getFavorites().add(store); // idempotent: Set ensures no duplicates
    authUserRepository.save(user);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{storeId}")
  public ResponseEntity<Void> removeFavorite(
      @RequestHeader("Authorization") String authHeader, @PathVariable java.util.UUID storeId) {
    AuthUser user = authUserResolver.requireUser(authHeader);
    thriftStoreRepository
        .findById(storeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    user.getFavorites().removeIf(ts -> ts.getId().equals(storeId)); // idempotent removal
    authUserRepository.save(user);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/versioned")
  public FavoritesVersionDto getFavoritesVersioned(
      @RequestHeader("Authorization") String authHeader) {
    AuthUser user = authUserResolver.requireUser(authHeader);
    var ids = user.getFavorites().stream().map(f -> f.getId().toString()).sorted().toList();
    String version = Integer.toHexString(ids.hashCode());
    return new FavoritesVersionDto(ids, version);
  }

 
}
