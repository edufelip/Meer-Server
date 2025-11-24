package com.edufelip.meer.domain;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;

public class CreateGuideContentUseCase {
    private final GuideContentRepository guideContentRepository;
    private final ThriftStoreRepository thriftStoreRepository;

    public CreateGuideContentUseCase(GuideContentRepository guideContentRepository, ThriftStoreRepository thriftStoreRepository) {
        this.guideContentRepository = guideContentRepository;
        this.thriftStoreRepository = thriftStoreRepository;
    }

    public GuideContent execute(GuideContent guideContent) {
        if (guideContent.getThriftStore() != null && guideContent.getThriftStore().getId() != null) {
            var thrift = thriftStoreRepository.findById(guideContent.getThriftStore().getId())
                    .orElseThrow(() -> new RuntimeException("Thrift store not found"));
            guideContent.setThriftStore(thrift);
        }
        return guideContentRepository.save(guideContent);
    }
}
