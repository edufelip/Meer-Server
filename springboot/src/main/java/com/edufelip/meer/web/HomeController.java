package com.edufelip.meer.web;

import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.dto.FeaturedStoreDto;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.HomeResponse;
import com.edufelip.meer.dto.NearbyStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  private final GetThriftStoresUseCase getThriftStoresUseCase;
  private final GetGuideContentUseCase getGuideContentUseCase;
  private final AuthUserResolver authUserResolver;
  private final StoreFeedbackService storeFeedbackService;
  private final GuideContentEngagementService guideContentEngagementService;

  public HomeController(
      GetThriftStoresUseCase getThriftStoresUseCase,
      GetGuideContentUseCase getGuideContentUseCase,
      AuthUserResolver authUserResolver,
      StoreFeedbackService storeFeedbackService,
      GuideContentEngagementService guideContentEngagementService) {
    this.getThriftStoresUseCase = getThriftStoresUseCase;
    this.getGuideContentUseCase = getGuideContentUseCase;
    this.authUserResolver = authUserResolver;
    this.storeFeedbackService = storeFeedbackService;
    this.guideContentEngagementService = guideContentEngagementService;
  }

  @GetMapping("/home")
  public HomeResponse home(
      @RequestHeader(name = "Authorization", required = false) String authHeader,
      @RequestParam(name = "lat") double lat,
      @RequestParam(name = "lng") double lng) {
    var user = authUserResolver.optionalUser(authHeader);

    var featuredStores = getThriftStoresUseCase.executeRecentTop10();
    var nearbyStores = getThriftStoresUseCase.executeNearby(lat, lng, 0, 10).getContent();

    var summaries =
        storeFeedbackService.getSummaries(
            java.util.stream.Stream.concat(featuredStores.stream(), nearbyStores.stream())
                .map(s -> s.getId())
                .distinct()
                .toList());

    var featuredDtos = featuredStores.stream().map(FeaturedStoreDto::new).toList();

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
                  return new NearbyStoreDto(
                      s, lat, lng, Mappers.isFavorite(user, s.getId()), rating, reviewCount);
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
}
