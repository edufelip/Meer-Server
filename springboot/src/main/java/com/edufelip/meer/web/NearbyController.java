package com.edufelip.meer.web;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.NearbyStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.StoreFeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class NearbyController {

    private final GetThriftStoresUseCase getThriftStoresUseCase;
    private final TokenProvider tokenProvider;
    private final AuthUserRepository authUserRepository;
    private final StoreFeedbackService storeFeedbackService;

    public NearbyController(GetThriftStoresUseCase getThriftStoresUseCase,
                            TokenProvider tokenProvider,
                            AuthUserRepository authUserRepository,
                            StoreFeedbackService storeFeedbackService) {
        this.getThriftStoresUseCase = getThriftStoresUseCase;
        this.tokenProvider = tokenProvider;
        this.authUserRepository = authUserRepository;
        this.storeFeedbackService = storeFeedbackService;
    }

    @GetMapping("/nearby")
    public PageResponse<NearbyStoreDto> nearby(@RequestHeader("Authorization") String authHeader,
                                               @RequestParam(name = "lat") double lat,
                                               @RequestParam(name = "lng") double lng,
                                               @RequestParam(name = "pageIndex", defaultValue = "0") int pageIndex,
                                               @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        if (pageIndex < 0 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
        }
        var user = currentUser(authHeader);
        Set<java.util.UUID> favoriteIds = user.getFavorites().stream().map(f -> f.getId()).collect(Collectors.toSet());

        var page = getThriftStoresUseCase.executeNearby(lat, lng, pageIndex, pageSize);
        List<ThriftStore> stores = page.getContent();

        var summaries = storeFeedbackService.getSummaries(stores.stream().map(ThriftStore::getId).toList());

        var items = stores.stream().map(store -> {
            var summary = summaries.get(store.getId());
            Double rating = summary != null ? summary.rating() : null;
            Integer reviewCount = summary != null && summary.reviewCount() != null ? summary.reviewCount().intValue() : null;
            Double distanceMeters = (store.getLatitude() != null && store.getLongitude() != null)
                    ? distanceKm(lat, lng, store.getLatitude(), store.getLongitude()) * 1000
                    : null;
            Integer walkMinutes = distanceMeters != null ? (int) Math.round(distanceMeters / 80.0) : null;
            return new NearbyStoreDto(
                    store.getId(),
                    store.getName(),
                    store.getDescription(),
                    firstPhotoOrCover(store),
                    store.getAddressLine(),
                    store.getLatitude(),
                    store.getLongitude(),
                    store.getNeighborhood(),
                    favoriteIds.contains(store.getId()),
                    store.getCategories(),
                    rating,
                    reviewCount,
                    distanceMeters,
                    walkMinutes
            );
        }).toList();

        return new PageResponse<>(items, pageIndex, page.hasNext());
    }

    // Haversine
    private double distanceKm(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) return Double.MAX_VALUE;
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private com.edufelip.meer.core.auth.AuthUser currentUser(String authHeader) {
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

    private String firstPhotoOrCover(ThriftStore store) {
        if (store.getPhotos() != null && !store.getPhotos().isEmpty()) {
            return store.getPhotos().get(0).getUrl();
        }
        return store.getCoverImageUrl();
    }
}
