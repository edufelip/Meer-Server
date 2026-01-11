package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.storage.AssetDeletionJob;
import com.edufelip.meer.core.storage.AssetDeletionStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetDeletionJobRepository extends JpaRepository<AssetDeletionJob, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        select j from AssetDeletionJob j
        where j.status in :statuses and j.nextAttemptAt <= :now
        order by j.nextAttemptAt asc, j.id asc
        """)
  List<AssetDeletionJob> findDueJobsForUpdate(
      @Param("statuses") List<AssetDeletionStatus> statuses,
      @Param("now") Instant now,
      Pageable pageable);
}
