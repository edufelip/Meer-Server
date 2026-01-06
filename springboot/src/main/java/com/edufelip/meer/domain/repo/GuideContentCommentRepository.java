package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContentComment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideContentCommentRepository extends JpaRepository<GuideContentComment, Integer> {
  Page<GuideContentComment> findByContentIdAndDeletedAtIsNull(Integer contentId, Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select c.content.id as contentId, count(c) as cnt
            from GuideContentComment c
            where c.content.id in :contentIds
              and c.deletedAt is null
            group by c.content.id
            """)
  List<CountView> countActiveByContentIds(
      @org.springframework.data.repository.query.Param("contentIds") List<Integer> contentIds);

  interface CountView {
    Integer getContentId();

    Long getCnt();
  }
}
