package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.FavoritesVersionDto;
import com.edufelip.meer.dto.FavoriteStoreDto;
import com.edufelip.meer.service.StoreFeedbackService;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/favorites")
public class FavoritesController {

    private final TokenProvider tokenProvider;
    private final AuthUserRepository authUserRepository;
    private final ThriftStoreRepository thriftStoreRepository;
    private final StoreFeedbackService storeFeedbackService;

    public FavoritesController(TokenProvider tokenProvider,
                               AuthUserRepository authUserRepository,
                               ThriftStoreRepository thriftStoreRepository,
                               StoreFeedbackService storeFeedbackService) {
        this.tokenProvider = tokenProvider;
        this.authUserRepository = authUserRepository;
        this.thriftStoreRepository = thriftStoreRepository;
        this.storeFeedbackService = storeFeedbackService;
    }

    @GetMapping
    public List<FavoriteStoreDto> listFavorites(@RequestHeader("Authorization") String authHeader,
                                                @RequestParam(name = "lat", required = false) Double lat,
                                                @RequestParam(name = "lng", required = false) Double lng) {
        AuthUser user = currentUser(authHeader);
        var ids = user.getFavorites().stream().map(f -> f.getId()).toList();
        var summaries = storeFeedbackService.getSummaries(ids);
        return user.getFavorites()
                .stream()
                .map(store -> {
                    var summary = summaries.get(store.getId());
                    Double rating = summary != null ? summary.rating() : null;
                    Integer reviewCount = summary != null && summary.reviewCount() != null ? summary.reviewCount().intValue() : null;
                    Double distanceMeters = (lat != null && lng != null && store.getLatitude() != null && store.getLongitude() != null)
                            ? distanceKm(lat, lng, store.getLatitude(), store.getLongitude()) * 1000
                            : null;
                    return new FavoriteStoreDto(
                            store.getId(),
                            store.getName(),
                            store.getDescription(),
                            firstPhotoOrCover(store),
                            store.getLatitude(),
                            store.getLongitude(),
                            true,
                            distanceMeters
                    );
                })
                .toList();
    }

    @PostMapping("/{storeId}")
    public ResponseEntity<Void> addFavorite(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable java.util.UUID storeId) {
        AuthUser user = currentUser(authHeader);
        var store = thriftStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        user.getFavorites().add(store); // idempotent: Set ensures no duplicates
        authUserRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> removeFavorite(@RequestHeader("Authorization") String authHeader,
                                               @PathVariable java.util.UUID storeId) {
        AuthUser user = currentUser(authHeader);
        thriftStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        user.getFavorites().removeIf(ts -> ts.getId().equals(storeId)); // idempotent removal
        authUserRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/versioned")
    public FavoritesVersionDto getFavoritesVersioned(@RequestHeader("Authorization") String authHeader) {
        AuthUser user = currentUser(authHeader);
        var ids = user.getFavorites().stream()
                .map(f -> f.getId().toString())
                .sorted()
                .toList();
        String version = Integer.toHexString(ids.hashCode());
        return new FavoritesVersionDto(ids, version);
    }

    private AuthUser currentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new InvalidTokenException();
        String token = authHeader.substring("Bearer ".length()).trim();
        TokenPayload payload;
        try {
            payload = tokenProvider.parseAccessToken(token);
        } catch (RuntimeException ex) {
            throw new InvalidTokenException();
        }
        return authUserRepository.findById(payload.getUserId()).orElseThrow(InvalidTokenException::new);
    }

    private Double distanceKm(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) return null;
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String firstPhotoOrCover(com.edufelip.meer.core.store.ThriftStore store) {
        if (store.getPhotos() != null && !store.getPhotos().isEmpty()) {
            return store.getPhotos().get(0).getUrl();
        }
        return store.getCoverImageUrl();
    }
}
