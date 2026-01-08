package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DeleteThriftStoreUseCase {
  private final ThriftStoreRepository thriftStoreRepository;
  private final AuthUserRepository authUserRepository;
  private final StoreFeedbackRepository storeFeedbackRepository;
  private final PhotoStoragePort photoStoragePort;
  private final StoreOwnershipService storeOwnershipService;

  public DeleteThriftStoreUseCase(
      ThriftStoreRepository thriftStoreRepository,
      AuthUserRepository authUserRepository,
      StoreFeedbackRepository storeFeedbackRepository,
      PhotoStoragePort photoStoragePort,
      StoreOwnershipService storeOwnershipService) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.authUserRepository = authUserRepository;
    this.storeFeedbackRepository = storeFeedbackRepository;
    this.photoStoragePort = photoStoragePort;
    this.storeOwnershipService = storeOwnershipService;
  }

  @Transactional
  public void execute(AuthUser user, UUID storeId) {
    ThriftStore store =
        thriftStoreRepository
            .findById(storeId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));

    if (!storeOwnershipService.isAdmin(user)
        && (user.getOwnedThriftStore() == null
            || !storeId.equals(user.getOwnedThriftStore().getId()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
    }

    // Detach owner linkage to avoid FK violations
    if (store.getOwner() != null) {
      var owner = store.getOwner();
      if (owner.getOwnedThriftStore() != null
          && storeId.equals(owner.getOwnedThriftStore().getId())) {
        owner.setOwnedThriftStore(null);
        authUserRepository.save(owner);
      }
    }

    authUserRepository.deleteFavoritesByStoreId(storeId);
    storeFeedbackRepository.deleteByThriftStoreId(storeId);

    if (store.getPhotos() != null) {
      store.getPhotos().forEach(p -> photoStoragePort.deleteByUrl(p.getUrl()));
    }
    thriftStoreRepository.delete(store);
  }
}
