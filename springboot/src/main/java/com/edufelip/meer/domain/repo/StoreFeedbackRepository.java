package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.dto.StoreRatingDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreFeedbackRepository extends JpaRepository<StoreFeedback, Integer> {
    Optional<StoreFeedback> findByUserIdAndThriftStoreId(UUID userId, UUID storeId);
    void deleteByUserId(UUID userId);

    interface AggregateView {
        java.util.UUID getStoreId();
        Double getAvgScore();
        Long getCnt();
    }

    void deleteByThriftStoreId(UUID storeId);

    @Query("select f.thriftStore.id as storeId, avg(f.score) as avgScore, count(f) as cnt from StoreFeedback f where f.thriftStore.id in :storeIds and f.score is not null group by f.thriftStore.id")
    List<AggregateView> aggregateByStoreIds(@Param("storeIds") List<UUID> storeIds);

    @Query("""
            select new com.edufelip.meer.dto.StoreRatingDto(
                f.id,
                s.id,
                f.score,
                f.body,
                u.displayName,
                u.photoUrl,
                f.createdAt
            )
            from StoreFeedback f
            join f.thriftStore s
            join f.user u
            where s.id = :storeId
              and f.score is not null
            order by f.createdAt desc
            """)
    Slice<StoreRatingDto> findRatingsByStoreId(@Param("storeId") UUID storeId, Pageable pageable);
}
