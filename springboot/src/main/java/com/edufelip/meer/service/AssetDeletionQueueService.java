package com.edufelip.meer.service;

import com.edufelip.meer.core.storage.AssetDeletionJob;
import com.edufelip.meer.core.storage.AssetDeletionStatus;
import com.edufelip.meer.domain.port.AssetDeletionQueuePort;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.AssetDeletionJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AssetDeletionQueueService implements AssetDeletionQueuePort {
  private static final Logger log = LoggerFactory.getLogger(AssetDeletionQueueService.class);

  private final AssetDeletionJobRepository assetDeletionJobRepository;
  private final PhotoStoragePort photoStoragePort;
  private final Clock clock;

  public AssetDeletionQueueService(
      AssetDeletionJobRepository assetDeletionJobRepository,
      PhotoStoragePort photoStoragePort,
      Clock clock) {
    this.assetDeletionJobRepository = assetDeletionJobRepository;
    this.photoStoragePort = photoStoragePort;
    this.clock = clock;
  }

  @Override
  public void enqueueAll(List<String> urls, String sourceType, String sourceId) {
    if (urls == null || urls.isEmpty()) return;
    Set<String> unique = new LinkedHashSet<>();
    for (String url : urls) {
      if (url == null || url.isBlank()) continue;
      if (!isManagedUrl(url)) {
        log.warn(
            "Skipping unmanaged asset URL sourceType={} sourceId={} url={}",
            sourceType,
            sourceId,
            url);
        continue;
      }
      unique.add(url);
    }
    if (unique.isEmpty()) return;

    Instant now = clock.instant();
    List<AssetDeletionJob> jobs =
        unique.stream().map(url -> new AssetDeletionJob(url, now, sourceType, sourceId)).toList();
    jobs.forEach(job -> job.setStatus(AssetDeletionStatus.PENDING));
    assetDeletionJobRepository.saveAll(jobs);
  }

  private boolean isManagedUrl(String url) {
    if (url == null) return false;
    if (url.startsWith("/uploads/")) return true;
    return photoStoragePort.extractFileKey(url) != null;
  }
}
