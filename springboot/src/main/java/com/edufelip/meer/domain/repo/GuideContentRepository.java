package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GuideContentRepository extends JpaRepository<GuideContent, Integer> {
    List<GuideContent> findByThriftStoreId(UUID thriftStoreId);
    List<GuideContent> findTop10ByOrderByCreatedAtDesc();
}
