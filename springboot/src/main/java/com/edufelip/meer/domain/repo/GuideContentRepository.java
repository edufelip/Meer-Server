package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface GuideContentRepository extends JpaRepository<GuideContent, Integer> {
    List<GuideContent> findByThriftStoreId(UUID thriftStoreId);
    List<GuideContent> findTop10ByOrderByCreatedAtDesc();
    Page<GuideContent> findByThriftStoreIdOrderByCreatedAtDesc(UUID thriftStoreId, Pageable pageable);
}
