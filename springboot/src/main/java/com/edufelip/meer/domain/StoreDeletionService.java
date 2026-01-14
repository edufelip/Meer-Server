package com.edufelip.meer.domain;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.port.AssetDeletionQueuePort;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StoreDeletionService {
  private final ThriftStoreRepository thriftStoreRepository;
  private final AuthUserRepository authUserRepository;
  private final StoreFeedbackRepository storeFeedbackRepository;
  private final GuideContentRepository guideContentRepository;
  private final AssetDeletionQueuePort assetDeletionQueuePort;

  public StoreDeletionService(
      ThriftStoreRepository thriftStoreRepository,
      AuthUserRepository authUserRepository,
      StoreFeedbackRepository storeFeedbackRepository,
      GuideContentRepository guideContentRepository,
      AssetDeletionQueuePort assetDeletionQueuePort) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.authUserRepository = authUserRepository;
    this.storeFeedbackRepository = storeFeedbackRepository;
    this.guideContentRepository = guideContentRepository;
    this.assetDeletionQueuePort = assetDeletionQueuePort;
  }

  public void deleteStoreWithAssets(ThriftStore store, Set<UUID> processed, String sourceType) {
    if (store == null || store.getId() == null) return;
    if (!processed.add(store.getId())) return;

    if (store.getOwner() != null) {
      var owner = store.getOwner();
      if (owner.getOwnedThriftStore() != null
          && store.getId().equals(owner.getOwnedThriftStore().getId())) {
        owner.setOwnedThriftStore(null);
        authUserRepository.save(owner);
      }
    }

    authUserRepository.deleteFavoritesByStoreId(store.getId());
    storeFeedbackRepository.deleteByThriftStoreId(store.getId());

    Set<String> assetUrls = collectStoreAssetUrls(store);
    guideContentRepository
        .findByThriftStoreId(store.getId())
        .forEach(content -> addUrl(assetUrls, content.getImageUrl()));

    assetDeletionQueuePort.enqueueAll(List.copyOf(assetUrls), sourceType, store.getId().toString());

    thriftStoreRepository.delete(store);
  }

  private Set<String> collectStoreAssetUrls(ThriftStore store) {
    Set<String> urls = new LinkedHashSet<>();
    addUrl(urls, store.getCoverImageUrl());
    if (store.getGalleryUrls() != null) {
      store.getGalleryUrls().forEach(url -> addUrl(urls, url));
    }
    if (store.getPhotos() != null) {
      store.getPhotos().forEach(photo -> addUrl(urls, photo != null ? photo.getUrl() : null));
    }
    return urls;
  }

  private void addUrl(Set<String> urls, String url) {
    if (url == null || url.isBlank()) return;
    urls.add(url);
  }
}
