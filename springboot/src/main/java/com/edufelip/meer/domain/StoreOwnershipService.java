package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class StoreOwnershipService {
  private final AuthUserRepository authUserRepository;
  private final ThriftStoreRepository thriftStoreRepository;

  public StoreOwnershipService(
      AuthUserRepository authUserRepository, ThriftStoreRepository thriftStoreRepository) {
    this.authUserRepository = authUserRepository;
    this.thriftStoreRepository = thriftStoreRepository;
  }

  public void ensureOwnerOrAdmin(AuthUser user, ThriftStore store) {
    if (isAdmin(user)) return;
    ensureOwner(user, store);
  }

  public void ensureOwnerOrAdminStrict(AuthUser user, ThriftStore store) {
    if (isAdmin(user)) return;
    if (!ownsStore(user, store)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
    }
  }

  public void ensureOwner(AuthUser user, ThriftStore store) {
    boolean userOwnsByLink =
        user.getOwnedThriftStore() != null
            && store.getId().equals(user.getOwnedThriftStore().getId());
    boolean userOwnsByStoreOwner =
        store.getOwner() != null && store.getOwner().getId().equals(user.getId());

    // If the links are missing but this user should own the store, repair the linkage eagerly.
    if (!userOwnsByLink && userOwnsByStoreOwner) {
      user.setOwnedThriftStore(store);
      authUserRepository.save(user);
      userOwnsByLink = true;
    } else if (!userOwnsByStoreOwner && userOwnsByLink) {
      store.setOwner(user);
      thriftStoreRepository.save(store);
      userOwnsByStoreOwner = true;
    } else if (!userOwnsByLink && !userOwnsByStoreOwner && store.getOwner() == null) {
      // freshly created store, set both sides
      store.setOwner(user);
      user.setOwnedThriftStore(store);
      thriftStoreRepository.save(store);
      authUserRepository.save(user);
      userOwnsByLink = true;
      userOwnsByStoreOwner = true;
    }

    if (!(userOwnsByLink || userOwnsByStoreOwner)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
    }
  }

  private boolean ownsStore(AuthUser user, ThriftStore store) {
    if (user == null || store == null) return false;
    boolean userOwnsByLink =
        user.getOwnedThriftStore() != null
            && store.getId().equals(user.getOwnedThriftStore().getId());
    boolean userOwnsByStoreOwner =
        store.getOwner() != null && store.getOwner().getId().equals(user.getId());
    return userOwnsByLink || userOwnsByStoreOwner;
  }

  public boolean isAdmin(AuthUser user) {
    return user != null && user.getRole() != null && user.getRole() == Role.ADMIN;
  }
}
