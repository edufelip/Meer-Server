package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UpdateThriftStoreUseCase {
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreOwnershipService storeOwnershipService;

  public record SocialUpdate(
      String facebook,
      boolean facebookPresent,
      String instagram,
      boolean instagramPresent,
      String website,
      boolean websitePresent,
      String whatsapp,
      boolean whatsappPresent) {}

  public record Command(
      String name,
      String description,
      String openingHours,
      String addressLine,
      Double latitude,
      Double longitude,
      String phone,
      String email,
      String neighborhood,
      Boolean isOnlineStore,
      java.util.List<String> categories,
      SocialUpdate social) {}

  public UpdateThriftStoreUseCase(
      ThriftStoreRepository thriftStoreRepository, StoreOwnershipService storeOwnershipService) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeOwnershipService = storeOwnershipService;
  }

  public ThriftStore execute(AuthUser user, UUID id, Command command) {
    ThriftStore store =
        thriftStoreRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    storeOwnershipService.ensureOwnerOrAdmin(user, store);

    if (command.name() != null && command.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
    }
    if (command.addressLine() != null && command.addressLine().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addressLine cannot be blank");
    }

    if (command.name() != null) store.setName(command.name());
    if (command.description() != null) store.setDescription(command.description());
    if (command.openingHours() != null) store.setOpeningHours(command.openingHours());
    if (command.addressLine() != null) store.setAddressLine(command.addressLine());
    if (command.isOnlineStore() != null) store.setIsOnlineStore(command.isOnlineStore());
    if (command.phone() != null) {
      var socialObj = store.getSocial() != null ? store.getSocial() : new Social();
      store.setSocial(socialObj);
      store.setPhone(command.phone());
    }
    if (command.email() != null) store.setEmail(command.email());
    if (command.neighborhood() != null) store.setNeighborhood(command.neighborhood());
    if (command.categories() != null) {
      store.setCategories(StoreCategoryNormalizer.normalize(command.categories()));
    }

    if (command.social() != null) {
      try {
        StoreSocialValidator.validate(
            command.social().facebook(), command.social().instagram(), command.social().website());
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
      }
      applySocialUpdates(store, command.social());
    }

    if (command.latitude() != null && command.longitude() != null) {
      store.setLatitude(command.latitude());
      store.setLongitude(command.longitude());
    } else if ((command.addressLine() != null)
        && (store.getLatitude() == null || store.getLongitude() == null)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "latitude/longitude required or geocoding failed");
    }

    thriftStoreRepository.save(store);
    return thriftStoreRepository.findById(id).orElseThrow();
  }

  private void applySocialUpdates(ThriftStore store, SocialUpdate updates) {
    Social social = store.getSocial();
    if (social == null) {
      social = new Social();
      store.setSocial(social);
    }
    if (updates.facebookPresent()) social.setFacebook(updates.facebook());
    if (updates.instagramPresent()) social.setInstagram(updates.instagram());
    if (updates.websitePresent()) social.setWebsite(updates.website());
    if (updates.whatsappPresent()) social.setWhatsapp(updates.whatsapp());
  }
}
