package com.edufelip.meer.domain;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.repo.GuideContentRepository;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;

public class GetGuideContentUseCase {
    private final GuideContentRepository guideContentRepository;

    public GetGuideContentUseCase(GuideContentRepository guideContentRepository) {
        this.guideContentRepository = guideContentRepository;
    }

    public GuideContent execute(Integer id) {
        return guideContentRepository.findById(id).orElse(null);
    }

    public List<GuideContent> executeAll() {
        return guideContentRepository.findAll();
    }

    @Cacheable("guideTop10")
    public List<GuideContent> executeRecentTop10() {
        return guideContentRepository.findTop10ByOrderByCreatedAtDesc();
    }
}
