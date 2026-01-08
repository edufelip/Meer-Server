package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.util.UrlValidatorUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UpdateGuideContentUseCase {
  private static final String DEFAULT_CATEGORY = "general";
  private static final String DEFAULT_TYPE = "article";

  private final GuideContentRepository guideContentRepository;
  private final StoreOwnershipService storeOwnershipService;

  public UpdateGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService) {
    this.guideContentRepository = guideContentRepository;
    this.storeOwnershipService = storeOwnershipService;
  }

  public com.edufelip.meer.core.content.GuideContent execute(
      AuthUser user, Integer contentId, Command command) {
    if (command == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    var content =
        guideContentRepository
            .findByIdAndDeletedAtIsNull(contentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    if (content.getThriftStore() == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to update content");
    }
    try {
      storeOwnershipService.ensureOwnerOrAdminStrict(user, content.getThriftStore());
    } catch (ResponseStatusException ex) {
      if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You must own this store to update content");
      }
      throw ex;
    }

    if (command.title() != null) {
      content.setTitle(command.title());
    }
    if (command.description() != null) {
      content.setDescription(command.description());
    }
    if (command.imageUrl() != null) {
      try {
        UrlValidatorUtil.ensureHttpUrl(command.imageUrl(), "imageUrl");
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
      }
      content.setImageUrl(command.imageUrl());
    }
    if (content.getCategoryLabel() == null) {
      content.setCategoryLabel(DEFAULT_CATEGORY);
    }
    if (content.getType() == null) {
      content.setType(DEFAULT_TYPE);
    }
    return guideContentRepository.save(content);
  }

  public record Command(String title, String description, String imageUrl) {}
}
