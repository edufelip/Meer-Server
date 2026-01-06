package com.edufelip.meer.web;

import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.HomeResponse;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  private final GetThriftStoresUseCase getThriftStoresUseCase;
  private final GetGuideContentUseCase getGuideContentUseCase;
  private final TokenProvider tokenProvider;
  private final AuthUserRepository authUserRepository;
  private final StoreFeedbackService storeFeedbackService;
  private final GuideContentEngagementService guideContentEngagementService;

  public HomeController(
      GetThriftStoresUseCase getThriftStoresUseCase,
      GetGuideContentUseCase getGuideContentUseCase,
      TokenProvider tokenProvider,
      AuthUserRepository authUserRepository,
      StoreFeedbackService storeFeedbackService,
      GuideContentEngagementService guideContentEngagementService) {
    this.getThriftStoresUseCase = getThriftStoresUseCase;
    this.getGuideContentUseCase = getGuideContentUseCase;
    this.tokenProvider = tokenProvider;
    this.authUserRepository = authUserRepository;
    this.storeFeedbackService = storeFeedbackService;
    this.guideContentEngagementService = guideContentEngagementService;
  }

  @GetMapping("/home")
  public HomeResponse home(
      @RequestHeader(name = "Authorization", required = false) String authHeader,
      @RequestParam(name = "lat") double lat,
      @RequestParam(name = "lng") double lng) {
    var user = currentUserOrNull(authHeader);
    Set<java.util.UUID> favoriteIds =
        user != null
            ? user.getFavorites().stream().map(f -> f.getId()).collect(Collectors.toSet())
            : Set.of();

    var featuredStores = getThriftStoresUseCase.executeRecentTop10();
    var nearbyStores = getThriftStoresUseCase.executeNearby(lat, lng, 0, 10).getContent();

    var summaries =
        storeFeedbackService.getSummaries(
            java.util.stream.Stream.concat(featuredStores.stream(), nearbyStores.stream())
                .map(s -> s.getId())
                .distinct()
                .toList());

    var featuredDtos =
        featuredStores.stream()
            .map(
                s ->
                    new com.edufelip.meer.dto.FeaturedStoreDto(
                        s.getId(),
                        s.getName(),
                        s.getPhotos() != null && !s.getPhotos().isEmpty()
                            ? s.getPhotos().get(0).getUrl()
                            : s.getCoverImageUrl()))
            .toList();

    var nearbyDtos =
        nearbyStores.stream()
            .map(
                s -> {
                  var summary = summaries.get(s.getId());
                  Double rating = summary != null ? summary.rating() : null;
                  Integer reviewCount =
                      summary != null && summary.reviewCount() != null
                          ? summary.reviewCount().intValue()
                          : null;
                  Double distanceMeters =
                      (s.getLatitude() != null && s.getLongitude() != null)
                          ? distanceKm(lat, lng, s.getLatitude(), s.getLongitude()) * 1000
                          : null;
                  Integer walkMinutes =
                      distanceMeters != null ? (int) Math.round(distanceMeters / 80.0) : null;
                  return new com.edufelip.meer.dto.NearbyStoreDto(
                      s.getId(),
                      s.getName(),
                      s.getDescription(),
                      s.getPhotos() != null && !s.getPhotos().isEmpty()
                          ? s.getPhotos().get(0).getUrl()
                          : s.getCoverImageUrl(),
                      s.getAddressLine(),
                      s.getLatitude(),
                      s.getLongitude(),
                      s.getNeighborhood(),
                      favoriteIds.contains(s.getId()),
                      s.getCategories(),
                      rating,
                      reviewCount,
                      distanceMeters,
                      walkMinutes);
                })
            .toList();

    var contents = getGuideContentUseCase.executeRecentTop10();
    var contentIds = contents.stream().map(gc -> gc.getId()).toList();
    var engagement =
        guideContentEngagementService.getEngagement(contentIds, user != null ? user.getId() : null);
    List<GuideContentDto> contentDtos =
        contents.stream()
            .map(
                gc -> {
                  var summary =
                      engagement.getOrDefault(
                          gc.getId(),
                          new GuideContentEngagementService.EngagementSummary(0L, 0L, false));
                  return Mappers.toDto(
                      gc, summary.likeCount(), summary.commentCount(), summary.likedByMe());
                })
            .toList();

    return new HomeResponse(featuredDtos, nearbyDtos, contentDtos);
  }

  // Haversine
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

  private ThriftStoreDto toStoreDto(
      com.edufelip.meer.core.store.ThriftStore s,
      Set<Integer> favoriteIds,
      java.util.Map<Integer, com.edufelip.meer.service.StoreFeedbackService.Summary> summaries,
      Double lat,
      Double lng) {
    var summary = summaries.get(s.getId());
    Double rating = summary != null ? summary.rating() : null;
    Integer reviewCount =
        summary != null && summary.reviewCount() != null ? summary.reviewCount().intValue() : null;
    Double distanceMeters =
        (lat != null && lng != null && s.getLatitude() != null && s.getLongitude() != null)
            ? distanceKm(lat, lng, s.getLatitude(), s.getLongitude()) * 1000
            : null;
    return Mappers.toDto(
        s, false, favoriteIds.contains(s.getId()), rating, reviewCount, distanceMeters);
  }
}
