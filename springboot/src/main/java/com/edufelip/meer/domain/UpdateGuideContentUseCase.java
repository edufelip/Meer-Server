package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.util.UrlValidatorUtil;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UpdateGuideContentUseCase {
  private static final String DEFAULT_CATEGORY = "general";
  private static final String DEFAULT_TYPE = "article";
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final GuideContentRepository guideContentRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final PhotoStoragePort photoStoragePort;

  public UpdateGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort) {
    this.guideContentRepository = guideContentRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.photoStoragePort = photoStoragePort;
  }

  public GuideContent execute(AuthUser user, Integer contentId, Command command) {
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
      String fileKey = photoStoragePort.extractFileKey(command.imageUrl());
      if (fileKey == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "imageUrl must belong to the configured storage bucket");
      }
      if (content.getThriftStore() != null && content.getThriftStore().getId() != null) {
        String expectedPrefix = "stores/" + content.getThriftStore().getId();
        if (!fileKey.startsWith(expectedPrefix)) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "imageUrl must belong to this store");
        }
      }
      var stored = photoStoragePort.fetchRequired(fileKey);
      String ctype = stored != null ? stored.contentType() : null;
      if (ctype == null || !SUPPORTED_CONTENT_TYPES.contains(ctype.toLowerCase())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "imageUrl must be jpeg, png or webp");
      }
      content.setImageUrl(photoStoragePort.publicUrl(fileKey));
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
