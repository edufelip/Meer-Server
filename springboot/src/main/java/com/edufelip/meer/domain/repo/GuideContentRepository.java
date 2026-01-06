package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.dto.GuideContentDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface GuideContentRepository extends JpaRepository<GuideContent, Integer> {
  java.util.Optional<GuideContent> findByIdAndDeletedAtIsNull(Integer id);

  @Modifying
  @Transactional
  @Query("update GuideContent c set c.likeCount = c.likeCount + 1 where c.id = :contentId")
  void incrementLikeCount(@Param("contentId") Integer contentId);

  @Modifying
  @Transactional
  @Query(
      "update GuideContent c set c.likeCount = case when c.likeCount > 0 then c.likeCount - 1 else 0 end where c.id = :contentId")
  void decrementLikeCount(@Param("contentId") Integer contentId);

  @Modifying
  @Transactional
  @Query("update GuideContent c set c.commentCount = c.commentCount + 1 where c.id = :contentId")
  void incrementCommentCount(@Param("contentId") Integer contentId);

  @Modifying
  @Transactional
  @Query(
      "update GuideContent c set c.commentCount = case when c.commentCount > 0 then c.commentCount - 1 else 0 end where c.id = :contentId")
  void decrementCommentCount(@Param("contentId") Integer contentId);

  List<GuideContent> findByThriftStoreId(UUID thriftStoreId);

  List<GuideContent> findByThriftStoreIdAndDeletedAtIsNull(UUID thriftStoreId);

  List<GuideContent> findTop10ByOrderByCreatedAtDesc();

  List<GuideContent> findTop10ByDeletedAtIsNullOrderByCreatedAtDesc();

  Page<GuideContent> findByThriftStoreIdOrderByCreatedAtDesc(UUID thriftStoreId, Pageable pageable);

  Page<GuideContent> findByThriftStoreIdAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID thriftStoreId, Pageable pageable);

  Page<GuideContent> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
      String title, String description, Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            where c.deletedAt is null
            """)
  Slice<GuideContentDto> findAllSummariesActive(Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            """)
  Slice<GuideContentDto> findAllSummaries(Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            where s.id = :storeId
              and c.deletedAt is null
            """)
  Slice<GuideContentDto> findAllSummariesByStoreIdActive(
      @org.springframework.data.repository.query.Param("storeId") UUID storeId, Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            where s.id = :storeId
            """)
  Slice<GuideContentDto> findAllSummariesByStoreId(
      @org.springframework.data.repository.query.Param("storeId") UUID storeId, Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            where c.deletedAt is null
              and (lower(c.title) like lower(concat('%', :q, '%'))
               or lower(c.description) like lower(concat('%', :q, '%')))
            """)
  Slice<GuideContentDto> searchSummariesActive(
      @org.springframework.data.repository.query.Param("q") String q, Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            where lower(c.title) like lower(concat('%', :q, '%'))
               or lower(c.description) like lower(concat('%', :q, '%'))
            """)
  Slice<GuideContentDto> searchSummaries(
      @org.springframework.data.repository.query.Param("q") String q, Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            where s.id = :storeId
              and c.deletedAt is null
              and (
                lower(c.title) like lower(concat('%', :q, '%'))
                or lower(c.description) like lower(concat('%', :q, '%'))
              )
            """)
  Slice<GuideContentDto> searchSummariesByStoreIdActive(
      @org.springframework.data.repository.query.Param("storeId") UUID storeId,
      @org.springframework.data.repository.query.Param("q") String q,
      Pageable pageable);

  @org.springframework.data.jpa.repository.Query(
      """
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt,
                0L,
                0L,
                false
            )
            from GuideContent c
            left join c.thriftStore s
            where s.id = :storeId
              and (
                lower(c.title) like lower(concat('%', :q, '%'))
                or lower(c.description) like lower(concat('%', :q, '%'))
              )
            """)
  Slice<GuideContentDto> searchSummariesByStoreId(
      @org.springframework.data.repository.query.Param("storeId") UUID storeId,
      @org.springframework.data.repository.query.Param("q") String q,
      Pageable pageable);
}
