package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuideContentRepository extends JpaRepository<GuideContent, Integer> {
    List<GuideContent> findByThriftStoreId(Integer thriftStoreId);
}
