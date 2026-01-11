package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DeleteThriftStoreUseCase {
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final StoreDeletionService storeDeletionService;

  public DeleteThriftStoreUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      StoreDeletionService storeDeletionService) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.storeDeletionService = storeDeletionService;
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

    storeDeletionService.deleteStoreWithAssets(store, new java.util.HashSet<>(), "STORE_DELETE");
  }
}
