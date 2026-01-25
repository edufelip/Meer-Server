package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.moderation.EntityType;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.service.moderation.ModerationPolicyService;
import com.edufelip.meer.util.UrlValidatorUtil;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UpdateGuideContentUseCase {
  private static final Logger log = LoggerFactory.getLogger(UpdateGuideContentUseCase.class);
  private static final String DEFAULT_CATEGORY = "general";
  private static final String DEFAULT_TYPE = "article";
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final long MAX_CONTENT_IMAGE_BYTES = 5 * 1024 * 1024L;

  private final GuideContentRepository guideContentRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final PhotoStoragePort photoStoragePort;
  private final ModerationPolicyService moderationPolicyService;

  public UpdateGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort,
      ModerationPolicyService moderationPolicyService) {
    this.guideContentRepository = guideContentRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.photoStoragePort = photoStoragePort;
    this.moderationPolicyService = moderationPolicyService;
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
    
    if (content.getThriftStore() != null) {
      try {
        storeOwnershipService.ensureOwnerOrAdminStrict(user, content.getThriftStore());
      } catch (ResponseStatusException ex) {
        if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
          throw new ResponseStatusException(
              HttpStatus.FORBIDDEN, "You must own this store to update content");
        }
        throw ex;
      }
    } else if (!storeOwnershipService.isAdmin(user)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to update content");
    }

    if (command.title() != null) {
      content.setTitle(command.title());
    }
    if (command.description() != null) {
      content.setDescription(command.description());
    }

    String previousImageUrl = content.getImageUrl();

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
      } else {
        // Global content images must have the 'global/' prefix
        if (!fileKey.startsWith("global/")) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "imageUrl must be a global content image");
        }
      }
      var stored = photoStoragePort.fetchRequired(fileKey);
      String ctype = stored != null ? stored.contentType() : null;
      if (ctype == null || !SUPPORTED_CONTENT_TYPES.contains(ctype.toLowerCase())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "imageUrl must be jpeg, png or webp");
      }
      Long size = stored != null ? stored.size() : null;
      if (size != null && size > MAX_CONTENT_IMAGE_BYTES) {
        throw new ResponseStatusException(
            HttpStatus.PAYLOAD_TOO_LARGE, "imageUrl too large (max 5MB)");
      }
      content.setImageUrl(photoStoragePort.publicUrl(fileKey));
    }
    if (content.getCategoryLabel() == null) {
      content.setCategoryLabel(DEFAULT_CATEGORY);
    }
    if (content.getType() == null) {
      content.setType(DEFAULT_TYPE);
    }

    GuideContent saved = guideContentRepository.save(content);

    // Enqueue new image for moderation if it changed
    String newImageUrl = saved.getImageUrl();
    if (newImageUrl != null && !newImageUrl.equals(previousImageUrl)) {
      try {
        moderationPolicyService.enqueueForModeration(
            newImageUrl, EntityType.GUIDE_CONTENT_IMAGE, saved.getId().toString());
        log.info(
            "Enqueued guide content image for moderation: contentId={}, url={}",
            saved.getId(),
            newImageUrl);
      } catch (Exception e) {
        log.error(
            "Failed to enqueue guide content image for moderation: contentId={}, url={}",
            saved.getId(),
            newImageUrl,
            e);
      }
    }

    return saved;
  }

  public record Command(String title, String description, String imageUrl) {}
}
