package com.edufelip.meer.domain;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.util.List;

public class GetGuideContentsByThriftStoreUseCase {
    private final GuideContentRepository guideContentRepository;

    public GetGuideContentsByThriftStoreUseCase(GuideContentRepository guideContentRepository) {
        this.guideContentRepository = guideContentRepository;
    }

    public List<GuideContent> execute(Integer thriftStoreId) {
        return guideContentRepository.findByThriftStoreId(thriftStoreId);
    }
}
