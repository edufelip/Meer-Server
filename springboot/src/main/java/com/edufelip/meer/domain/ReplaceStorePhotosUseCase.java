package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ReplaceStorePhotosUseCase {
  private static final Logger log = LoggerFactory.getLogger(ReplaceStorePhotosUseCase.class);

  public record PhotoItem(Integer photoId, String fileKey, Integer position) {}

  public record Command(List<PhotoItem> photos, List<Integer> deletePhotoIds) {}

  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final PhotoStoragePort photoStoragePort;

  public ReplaceStorePhotosUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.photoStoragePort = photoStoragePort;
  }

  public ThriftStore execute(AuthUser user, UUID storeId, Command command) {
    var store =
        thriftStoreRepository
            .findById(storeId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    storeOwnershipService.ensureOwnerOrAdmin(user, store);

    List<PhotoItem> items = command.photos() != null ? new ArrayList<>(command.photos()) : null;
    if (items == null || items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photos array is required");
    }
    if (items.size() > StorePhotoPolicy.MAX_PHOTO_COUNT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Too many photos; max " + StorePhotoPolicy.MAX_PHOTO_COUNT);
    }

    // validate unique contiguous positions
    var positions = new java.util.HashSet<Integer>();
    for (var i : items) {
      if (i.position() == null || i.position() < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "position must be non-negative");
      }
      if (!positions.add(i.position())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "duplicate position " + i.position());
      }
    }
    int max = positions.stream().mapToInt(Integer::intValue).max().orElse(0);
    if (max != items.size() - 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "positions must be contiguous starting at 0");
    }

    var existingPhotos =
        store.getPhotos() == null
            ? new ArrayList<ThriftStorePhoto>()
            : new ArrayList<>(store.getPhotos());
    var existingById = new java.util.HashMap<Integer, ThriftStorePhoto>();
    for (var p : existingPhotos) {
      existingById.put(p.getId(), p);
    }

    // handle deletions
    List<Integer> deleteIds = command.deletePhotoIds();
    if (deleteIds != null && !deleteIds.isEmpty()) {
      for (Integer delId : deleteIds) {
        var removed = existingById.remove(delId);
        if (removed != null) {
          log.info("Deleting store photo (explicit) storeId={} photoId={}", storeId, delId);
          photoStoragePort.deleteByUrl(removed.getUrl());
        }
      }
    }

    List<ThriftStorePhoto> finalPhotos = new ArrayList<>();
    items.sort(Comparator.comparing(PhotoItem::position));

    for (var item : items) {
      boolean hasPhotoId = item.photoId() != null;
      boolean hasFileKey = item.fileKey() != null && !item.fileKey().isBlank();
      if (!hasPhotoId && !hasFileKey) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Each photo must have photoId or fileKey");
      }

      if (hasPhotoId) {
        var existing = existingById.remove(item.photoId());
        if (existing == null) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "photoId " + item.photoId() + " not found on this store");
        }
        existing.setDisplayOrder(item.position());
        finalPhotos.add(existing);
        continue;
      }

      // new photo via fileKey
      if (!item.fileKey().startsWith("stores/" + storeId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "fileKey does not belong to this store");
      }
      var storedObject = photoStoragePort.fetchRequired(item.fileKey());
      validateStoredObject(storedObject, item.fileKey());
      String viewUrl = photoStoragePort.publicUrl(item.fileKey());
      finalPhotos.add(new ThriftStorePhoto(store, viewUrl, item.position()));
    }

    // Mutate managed collection in place to avoid orphanRemoval issues
    var photos = store.getPhotos();
    if (photos == null) {
      photos = new ArrayList<>();
      store.setPhotos(photos);
    } else {
      photos.clear();
    }
    photos.addAll(finalPhotos);
    photos.sort(
        Comparator.comparing(
            p -> p.getDisplayOrder() == null ? Integer.MAX_VALUE : p.getDisplayOrder()));
    if (!photos.isEmpty()) {
      store.setCoverImageUrl(photos.get(0).getUrl());
    } else {
      store.setCoverImageUrl(null);
    }
    thriftStoreRepository.save(store);

    // Clean up any photos not kept nor explicitly deleted (implicit removals)
    if (!existingById.isEmpty()) {
      existingById
          .values()
          .forEach(
              photo -> {
                log.info(
                    "Deleting store photo (implicit) storeId={} photoId={}",
                    storeId,
                    photo.getId());
                photoStoragePort.deleteByUrl(photo.getUrl());
              });
    }

    return thriftStoreRepository.findById(storeId).orElseThrow();
  }

  private void validateStoredObject(PhotoStoragePort.StoredObject storedObject, String fileKey) {
    String ctype = storedObject != null ? storedObject.contentType() : null;
    if (!StorePhotoPolicy.isSupportedContentType(ctype)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported content type for " + fileKey);
    }
    Long size = storedObject != null ? storedObject.size() : null;
    if (size != null && size > StorePhotoPolicy.MAX_PHOTO_BYTES) {
      throw new ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE, "File too large (>2MB) for " + fileKey);
    }
  }
}
