package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.service.GuideContentModerationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DeleteGuideContentUseCase {
  private final GuideContentRepository guideContentRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final GuideContentModerationService guideContentModerationService;

  public DeleteGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService,
      GuideContentModerationService guideContentModerationService) {
    this.guideContentRepository = guideContentRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.guideContentModerationService = guideContentModerationService;
  }

  public void execute(AuthUser user, Integer contentId) {
    var content =
        guideContentRepository
            .findById(contentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    if (content.getThriftStore() == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to delete content");
    }
    try {
      storeOwnershipService.ensureOwnerOrAdminStrict(user, content.getThriftStore());
    } catch (ResponseStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You must own this store to delete content");
      }
      throw ex;
    }
    guideContentModerationService.softDeleteContent(content, user, null);
  }
}
