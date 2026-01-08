package com.edufelip.meer.domain;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.service.GuideContentEngagementService;
import java.util.List;
import java.util.UUID;

public class GetStoreContentsUseCase {

  public record ContentItem(
      GuideContent content, GuideContentEngagementService.EngagementSummary engagement) {}

  private final GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase;
  private final GuideContentEngagementService guideContentEngagementService;

  public GetStoreContentsUseCase(
      GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase,
      GuideContentEngagementService guideContentEngagementService) {
    this.getGuideContentsByThriftStoreUseCase = getGuideContentsByThriftStoreUseCase;
    this.guideContentEngagementService = guideContentEngagementService;
  }

  public List<ContentItem> execute(UUID storeId, UUID userId) {
    var contents = getGuideContentsByThriftStoreUseCase.execute(storeId);
    var contentIds = contents.stream().map(GuideContent::getId).toList();
    var engagement = guideContentEngagementService.getEngagement(contentIds, userId);
    return contents.stream()
        .map(
            content ->
                new ContentItem(
                    content,
                    engagement.getOrDefault(
                        content.getId(),
                        new GuideContentEngagementService.EngagementSummary(0L, 0L, false))))
        .toList();
  }
}
