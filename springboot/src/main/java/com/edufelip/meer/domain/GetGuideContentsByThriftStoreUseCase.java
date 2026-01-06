package com.edufelip.meer.domain;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.util.List;
import java.util.UUID;

public class GetGuideContentsByThriftStoreUseCase {
  private final GuideContentRepository guideContentRepository;

  public GetGuideContentsByThriftStoreUseCase(GuideContentRepository guideContentRepository) {
    this.guideContentRepository = guideContentRepository;
  }

  public List<GuideContent> execute(UUID thriftStoreId) {
    return guideContentRepository.findByThriftStoreIdAndDeletedAtIsNull(thriftStoreId);
  }
}
