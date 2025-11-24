package com.edufelip.meer.domain;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.repo.GuideContentRepository;

public class GetGuideContentUseCase {
    private final GuideContentRepository guideContentRepository;

    public GetGuideContentUseCase(GuideContentRepository guideContentRepository) {
        this.guideContentRepository = guideContentRepository;
    }

    public GuideContent execute(Integer id) {
        return guideContentRepository.findById(id).orElse(null);
    }
}
