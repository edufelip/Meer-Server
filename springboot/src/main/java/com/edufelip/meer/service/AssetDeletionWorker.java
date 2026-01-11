package com.edufelip.meer.service;

import com.edufelip.meer.core.storage.AssetDeletionJob;
import com.edufelip.meer.core.storage.AssetDeletionStatus;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.AssetDeletionJobRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AssetDeletionWorker {
  private static final Logger log = LoggerFactory.getLogger(AssetDeletionWorker.class);

  private final AssetDeletionJobRepository assetDeletionJobRepository;
  private final PhotoStoragePort photoStoragePort;
  private final Clock clock;
  private final int batchSize;
  private final int maxAttempts;
  private final Duration baseBackoff;
  private final Duration maxBackoff;

  public AssetDeletionWorker(
      AssetDeletionJobRepository assetDeletionJobRepository,
      PhotoStoragePort photoStoragePort,
      Clock clock,
      @Value("${asset-deletion.worker.batch-size:25}") int batchSize,
      @Value("${asset-deletion.worker.max-attempts:8}") int maxAttempts,
      @Value("${asset-deletion.worker.base-backoff-seconds:30}") long baseBackoffSeconds,
      @Value("${asset-deletion.worker.max-backoff-seconds:3600}") long maxBackoffSeconds) {
    this.assetDeletionJobRepository = assetDeletionJobRepository;
    this.photoStoragePort = photoStoragePort;
    this.clock = clock;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.baseBackoff = Duration.ofSeconds(baseBackoffSeconds);
    this.maxBackoff = Duration.ofSeconds(maxBackoffSeconds);
  }

  @Scheduled(fixedDelayString = "${asset-deletion.worker.delay-ms:30000}")
  @Transactional
  public void processQueue() {
    Instant now = clock.instant();
    List<AssetDeletionJob> jobs =
        assetDeletionJobRepository.findDueJobsForUpdate(
            List.of(
                AssetDeletionStatus.PENDING,
                AssetDeletionStatus.RETRY,
                AssetDeletionStatus.IN_PROGRESS),
            now,
            PageRequest.of(0, batchSize));

    for (AssetDeletionJob job : jobs) {
      processJob(job, now);
    }
  }

  private void processJob(AssetDeletionJob job, Instant now) {
    if (job == null || job.getUrl() == null || job.getUrl().isBlank()) {
      markFailed(job, now, "empty-url");
      return;
    }
    job.setStatus(AssetDeletionStatus.IN_PROGRESS);
    assetDeletionJobRepository.save(job);
    try {
      photoStoragePort.deleteByUrl(job.getUrl());
      job.setStatus(AssetDeletionStatus.SUCCEEDED);
      job.setLastError(null);
      job.setNextAttemptAt(now);
      assetDeletionJobRepository.save(job);
    } catch (Exception ex) {
      markRetryOrFail(job, now, ex.getMessage());
    }
  }

  private void markRetryOrFail(AssetDeletionJob job, Instant now, String error) {
    int nextAttempt = job.getAttempts() + 1;
    job.setAttempts(nextAttempt);
    job.setLastError(error);
    if (nextAttempt >= maxAttempts) {
      job.setStatus(AssetDeletionStatus.FAILED);
      job.setNextAttemptAt(now);
      log.warn(
          "Asset deletion failed permanently id={} url={} attempts={} error={}",
          job.getId(),
          job.getUrl(),
          nextAttempt,
          error);
      assetDeletionJobRepository.save(job);
      return;
    }
    Duration backoff = computeBackoff(nextAttempt);
    job.setStatus(AssetDeletionStatus.RETRY);
    job.setNextAttemptAt(now.plus(backoff));
    log.warn(
        "Asset deletion failed id={} url={} attempts={} nextAttemptIn={}s error={}",
        job.getId(),
        job.getUrl(),
        nextAttempt,
        backoff.getSeconds(),
        error);
    assetDeletionJobRepository.save(job);
  }

  private void markFailed(AssetDeletionJob job, Instant now, String error) {
    if (job == null) return;
    job.setStatus(AssetDeletionStatus.FAILED);
    job.setAttempts(job.getAttempts() + 1);
    job.setLastError(error);
    job.setNextAttemptAt(now);
    assetDeletionJobRepository.save(job);
  }

  private Duration computeBackoff(int attempt) {
    long multiplier = 1L << Math.min(attempt - 1, 10);
    long seconds = Math.min(baseBackoff.getSeconds() * multiplier, maxBackoff.getSeconds());
    return Duration.ofSeconds(seconds);
  }
}
