package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CreateThriftStoreUseCase {
  private final ThriftStoreRepository thriftStoreRepository;
  private final AuthUserRepository authUserRepository;

  public record SocialInput(String facebook, String instagram, String website, String whatsapp) {}

  public record Command(
      String name,
      String description,
      String openingHours,
      String addressLine,
      Double latitude,
      Double longitude,
      String phone,
      String email,
      String tagline,
      String neighborhood,
      java.util.List<String> categories,
      SocialInput social) {}

  public CreateThriftStoreUseCase(
      ThriftStoreRepository thriftStoreRepository, AuthUserRepository authUserRepository) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.authUserRepository = authUserRepository;
  }

  @CacheEvict(cacheNames = "featuredTop10", allEntries = true)
  public ThriftStore execute(AuthUser user, Command command) {
    validateCreate(command);
    try {
      StoreSocialValidator.validate(
          command.social() != null ? command.social().facebook() : null,
          command.social() != null ? command.social().instagram() : null,
          command.social() != null ? command.social().website() : null);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    ThriftStore store = new ThriftStore();
    store.setName(command.name());
    store.setDescription(command.description());
    store.setOpeningHours(command.openingHours());
    store.setAddressLine(command.addressLine());
    store.setLatitude(command.latitude());
    store.setLongitude(command.longitude());
    store.setOwner(user);
    store.setSocial(toSocial(command.social()));
    store.setPhone(command.phone());
    store.setEmail(command.email());
    store.setTagline(command.tagline());
    store.setNeighborhood(command.neighborhood());
    store.setCategories(StoreCategoryNormalizer.normalize(command.categories()));

    var saved = thriftStoreRepository.save(store);
    user.setOwnedThriftStore(saved);
    authUserRepository.save(user);
    return saved;
  }

  private Social toSocial(SocialInput input) {
    if (input == null) return null;
    Social social = new Social();
    social.setFacebook(input.facebook());
    social.setInstagram(input.instagram());
    social.setWebsite(input.website());
    social.setWhatsapp(input.whatsapp());
    return social;
  }

  private void validateCreate(Command command) {
    if (command == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    if (command.name() == null || command.name().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
    }
    if (command.addressLine() == null || command.addressLine().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addressLine is required");
    }
    if (command.description() == null || command.description().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
    }
    if (command.phone() == null || command.phone().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone is required");
    }
    if (command.latitude() == null || command.longitude() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "latitude/longitude required or geocoding failed");
    }
  }
}
