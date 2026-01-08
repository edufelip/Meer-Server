package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CreateStoreGuideContentUseCase {
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final CreateGuideContentUseCase createGuideContentUseCase;

  public CreateStoreGuideContentUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      CreateGuideContentUseCase createGuideContentUseCase) {
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.createGuideContentUseCase = createGuideContentUseCase;
  }

  public GuideContent execute(AuthUser user, UUID storeId, GuideContent guideContent) {
    var thriftStore =
        thriftStoreRepository
            .findById(storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    storeOwnershipService.ensureOwnerOrAdminStrict(user, thriftStore);
    var contentWithStore =
        new GuideContent(
            guideContent.getId(),
            guideContent.getTitle(),
            guideContent.getDescription(),
            guideContent.getCategoryLabel(),
            guideContent.getType(),
            guideContent.getImageUrl(),
            thriftStore);
    return createGuideContentUseCase.execute(contentWithStore);
  }
}
