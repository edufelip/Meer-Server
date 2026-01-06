package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContentLike;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideContentLikeRepository extends JpaRepository<GuideContentLike, Integer> {
  Optional<GuideContentLike> findByUserIdAndContentId(UUID userId, Integer contentId);

  boolean existsByUserIdAndContentId(UUID userId, Integer contentId);

  @org.springframework.data.jpa.repository.Query(
      """
            select l.content.id as contentId, count(l) as cnt
            from GuideContentLike l
            where l.content.id in :contentIds
            group by l.content.id
            """)
  List<CountView> countByContentIds(
      @org.springframework.data.repository.query.Param("contentIds") List<Integer> contentIds);

  @org.springframework.data.jpa.repository.Query(
      """
            select l.content.id
            from GuideContentLike l
            where l.user.id = :userId and l.content.id in :contentIds
            """)
  List<Integer> findLikedContentIds(
      @org.springframework.data.repository.query.Param("userId") UUID userId,
      @org.springframework.data.repository.query.Param("contentIds") List<Integer> contentIds);

  interface CountView {
    Integer getContentId();

    Long getCnt();
  }
}
