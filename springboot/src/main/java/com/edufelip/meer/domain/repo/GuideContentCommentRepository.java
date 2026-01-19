package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContentComment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GuideContentCommentRepository extends JpaRepository<GuideContentComment, Integer> {
  Page<GuideContentComment> findByContentId(Integer contentId, Pageable pageable);

  @Query(
      """
            select c.content.id as contentId, count(c) as cnt
            from GuideContentComment c
            where c.content.id in :contentIds
            group by c.content.id
            """)
  List<CountView> countByContentIds(@Param("contentIds") List<Integer> contentIds);

  @Query(
      value =
          """
                select c
                from GuideContentComment c
                join fetch c.user u
                join fetch c.content content
                left join fetch content.thriftStore store
                where (:contentId is null or content.id = :contentId)
                  and (:storeId is null or store.id = :storeId)
                  and (:from is null or c.createdAt >= :from)
                  and (:to is null or c.createdAt < :to)
                  and (
                    :search is null
                    or lower(c.body) like lower(concat('%', :search, '%'))
                    or lower(content.title) like lower(concat('%', :search, '%'))
                  )
                """,
      countQuery =
          """
                select count(c)
                from GuideContentComment c
                join c.content content
                left join content.thriftStore store
                where (:contentId is null or content.id = :contentId)
                  and (:storeId is null or store.id = :storeId)
                  and (:from is null or c.createdAt >= :from)
                  and (:to is null or c.createdAt < :to)
                  and (
                    :search is null
                    or lower(c.body) like lower(concat('%', :search, '%'))
                    or lower(content.title) like lower(concat('%', :search, '%'))
                  )
                """)
  Page<GuideContentComment> findDashboardComments(
      @Param("contentId") Integer contentId,
      @Param("storeId") UUID storeId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("search") String search,
      Pageable pageable);

  @Modifying
  @Transactional
  @Query("delete from GuideContentComment c where c.user.id = :userId")
  void deleteByUserId(@Param("userId") UUID userId);

  @Modifying
  @Transactional
  @Query("delete from GuideContentComment c where c.content.id in :contentIds")
  void deleteByContentIds(@Param("contentIds") List<Integer> contentIds);

  @Modifying
  @Transactional
  @Query("update GuideContentComment c set c.editedBy = null where c.editedBy.id = :userId")
  void clearEditedByUserId(@Param("userId") UUID userId);

  @Modifying
  @Transactional
  @Query(
      value =
          "update guide_content_comment set deleted_by_user_id = null where deleted_by_user_id = :userId",
      nativeQuery = true)
  void clearDeletedByUserId(@Param("userId") UUID userId);

  interface CountView {
    Integer getContentId();

    Long getCnt();
  }
}
