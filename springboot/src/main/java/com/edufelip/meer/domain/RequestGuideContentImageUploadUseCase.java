package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RequestGuideContentImageUploadUseCase {
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final GuideContentRepository guideContentRepository;
  private final StoreOwnershipService storeOwnershipService;
  private final PhotoStoragePort photoStoragePort;

  public RequestGuideContentImageUploadUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort) {
    this.guideContentRepository = guideContentRepository;
    this.storeOwnershipService = storeOwnershipService;
    this.photoStoragePort = photoStoragePort;
  }

  public PhotoStoragePort.UploadSlot execute(AuthUser user, Integer contentId, String contentType) {
    GuideContent content =
        guideContentRepository
            .findByIdAndDeletedAtIsNull(contentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    if (content.getThriftStore() == null || content.getThriftStore().getId() == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You must own this store to upload content images");
    }
    storeOwnershipService.ensureOwnerOrAdminStrict(user, content.getThriftStore());

    String normalized = normalizeContentType(contentType);
    List<String> contentTypes = normalized != null ? List.of(normalized) : null;
    return photoStoragePort
        .createUploadSlots(content.getThriftStore().getId(), 1, contentTypes)
        .get(0);
  }

  private String normalizeContentType(String contentType) {
    if (contentType == null) return null;
    String normalized = contentType.trim().toLowerCase();
    if (normalized.isBlank() || !SUPPORTED_CONTENT_TYPES.contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type");
    }
    return normalized;
  }
}
