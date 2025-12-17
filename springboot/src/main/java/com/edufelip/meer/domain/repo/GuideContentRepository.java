package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.dto.GuideContentDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import java.util.List;
import java.util.UUID;

public interface GuideContentRepository extends JpaRepository<GuideContent, Integer> {
    List<GuideContent> findByThriftStoreId(UUID thriftStoreId);
    List<GuideContent> findTop10ByOrderByCreatedAtDesc();
    Page<GuideContent> findByThriftStoreIdOrderByCreatedAtDesc(UUID thriftStoreId, Pageable pageable);
    Page<GuideContent> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt
            )
            from GuideContent c
            left join c.thriftStore s
            """)
    Slice<GuideContentDto> findAllSummaries(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt
            )
            from GuideContent c
            left join c.thriftStore s
            where s.id = :storeId
            """)
    Slice<GuideContentDto> findAllSummariesByStoreId(@org.springframework.data.repository.query.Param("storeId") UUID storeId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt
            )
            from GuideContent c
            left join c.thriftStore s
            where lower(c.title) like lower(concat('%', :q, '%'))
               or lower(c.description) like lower(concat('%', :q, '%'))
            """)
    Slice<GuideContentDto> searchSummaries(@org.springframework.data.repository.query.Param("q") String q, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select new com.edufelip.meer.dto.GuideContentDto(
                c.id,
                c.title,
                c.description,
                c.imageUrl,
                s.id,
                s.name,
                s.coverImageUrl,
                c.createdAt
            )
            from GuideContent c
            left join c.thriftStore s
            where s.id = :storeId
              and (
                lower(c.title) like lower(concat('%', :q, '%'))
                or lower(c.description) like lower(concat('%', :q, '%'))
              )
            """)
    Slice<GuideContentDto> searchSummariesByStoreId(@org.springframework.data.repository.query.Param("storeId") UUID storeId,
                                                    @org.springframework.data.repository.query.Param("q") String q,
                                                    Pageable pageable);
}
