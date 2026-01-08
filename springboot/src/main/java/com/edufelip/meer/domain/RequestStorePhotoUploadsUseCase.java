package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RequestStorePhotoUploadsUseCase {
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final PhotoStoragePort photoStoragePort;

  public RequestStorePhotoUploadsUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.photoStoragePort = photoStoragePort;
  }

  public List<PhotoStoragePort.UploadSlot> execute(
      AuthUser user, UUID storeId, Integer count, List<String> contentTypes) {
    var store =
        thriftStoreRepository
            .findById(storeId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    storeOwnershipService.ensureOwnerOrAdmin(user, store);

    int requested = count != null ? count : 0;
    if (requested <= 0 || requested > StorePhotoPolicy.MAX_PHOTO_COUNT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "count must be between 1 and " + StorePhotoPolicy.MAX_PHOTO_COUNT);
    }
    if (contentTypes != null) {
      for (String ct : contentTypes) {
        if (ct != null && !StorePhotoPolicy.isSupportedContentType(ct)) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Unsupported content type: " + ct);
        }
      }
    }
    int existing = store.getPhotos() == null ? 0 : store.getPhotos().size();
    if (existing + requested > StorePhotoPolicy.MAX_PHOTO_COUNT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Store already has " + existing + " photos; max " + StorePhotoPolicy.MAX_PHOTO_COUNT);
    }

    return photoStoragePort.createUploadSlots(storeId, requested, contentTypes);
  }
}
