package com.edufelip.meer.web;

import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.HomeResponse;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.service.StoreFeedbackService;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
public class HomeController {

    private final GetThriftStoresUseCase getThriftStoresUseCase;
    private final GetGuideContentUseCase getGuideContentUseCase;
    private final TokenProvider tokenProvider;
    private final AuthUserRepository authUserRepository;
    private final StoreFeedbackService storeFeedbackService;

    public HomeController(GetThriftStoresUseCase getThriftStoresUseCase,
                          GetGuideContentUseCase getGuideContentUseCase,
                          TokenProvider tokenProvider,
                          AuthUserRepository authUserRepository,
                          StoreFeedbackService storeFeedbackService) {
        this.getThriftStoresUseCase = getThriftStoresUseCase;
        this.getGuideContentUseCase = getGuideContentUseCase;
        this.tokenProvider = tokenProvider;
        this.authUserRepository = authUserRepository;
        this.storeFeedbackService = storeFeedbackService;
    }

    @GetMapping("/home")
    public HomeResponse home(@RequestHeader("Authorization") String authHeader,
                             @RequestParam(name = "lat", required = false) Double lat,
                             @RequestParam(name = "lng", required = false) Double lng) {
        var user = currentUser(authHeader);
        Set<Integer> favoriteIds = user.getFavorites().stream().map(f -> f.getId()).collect(Collectors.toSet());

        var stores = getThriftStoresUseCase.execute();
        var summaries = storeFeedbackService.getSummaries(stores.stream().map(s -> s.getId()).toList());

        var featuredStores = stores.stream()
                .filter(s -> s.getBadgeLabel() != null)
                .limit(10)
                .toList();
        if (featuredStores.isEmpty()) {
            featuredStores = stores.stream().limit(10).toList();
        }

        var nearbyStores = sortByDistanceIfPossible(stores, lat, lng).stream()
                .limit(10)
                .toList();

        var featuredDtos = featuredStores.stream().map(s -> toStoreDto(s, favoriteIds, summaries, lat, lng)).toList();
        var nearbyDtos = nearbyStores.stream().map(s -> toStoreDto(s, favoriteIds, summaries, lat, lng)).toList();

        List<GuideContentDto> contentDtos = getGuideContentUseCase.executeAll().stream()
                .limit(10)
                .map(Mappers::toDto)
                .toList();

        return new HomeResponse(featuredDtos, nearbyDtos, contentDtos);
    }

    private List<com.edufelip.meer.core.store.ThriftStore> sortByDistanceIfPossible(List<com.edufelip.meer.core.store.ThriftStore> stores, Double lat, Double lng) {
        if (lat == null || lng == null) return stores;
        return stores.stream()
                .sorted(Comparator.comparingDouble(s -> distanceKm(lat, lng, s.getLatitude(), s.getLongitude())))
                .toList();
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

    private ThriftStoreDto toStoreDto(com.edufelip.meer.core.store.ThriftStore s,
                                      Set<Integer> favoriteIds,
                                      java.util.Map<Integer, com.edufelip.meer.service.StoreFeedbackService.Summary> summaries,
                                      Double lat,
                                      Double lng) {
        var summary = summaries.get(s.getId());
        Double rating = summary != null ? summary.rating() : null;
        Integer reviewCount = summary != null && summary.reviewCount() != null ? summary.reviewCount().intValue() : null;
        Double distanceMeters = (lat != null && lng != null && s.getLatitude() != null && s.getLongitude() != null)
                ? distanceKm(lat, lng, s.getLatitude(), s.getLongitude()) * 1000
                : null;
        return Mappers.toDto(s, false, favoriteIds.contains(s.getId()), rating, reviewCount, distanceMeters);
    }
}
