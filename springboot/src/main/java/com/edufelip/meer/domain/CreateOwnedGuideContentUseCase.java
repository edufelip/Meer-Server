package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CreateOwnedGuideContentUseCase {
  private static final String DEFAULT_CATEGORY = "general";
  private static final String DEFAULT_TYPE = "article";

  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final CreateGuideContentUseCase createGuideContentUseCase;

  public CreateOwnedGuideContentUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      CreateGuideContentUseCase createGuideContentUseCase) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.createGuideContentUseCase = createGuideContentUseCase;
  }

  public GuideContent execute(AuthUser user, Command command) {
    if (command == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    if (command.title() == null || command.title().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
    }
    if (command.description() == null || command.description().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
    }
    if (command.storeId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId is required");
    }

    var thriftStore =
        thriftStoreRepository
            .findById(command.storeId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    try {
      storeOwnershipService.ensureOwnerOrAdminStrict(user, thriftStore);
    } catch (ResponseStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You must own this store to add content");
      }
      throw ex;
    }

    GuideContent content =
        new GuideContent(
            null,
            command.title(),
            command.description(),
            DEFAULT_CATEGORY,
            DEFAULT_TYPE,
            "",
            thriftStore);
    return createGuideContentUseCase.execute(content);
  }

  public record Command(String title, String description, UUID storeId) {}
}
