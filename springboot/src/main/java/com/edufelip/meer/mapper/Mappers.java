package com.edufelip.meer.mapper;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.category.Category;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.core.moderation.ImageModeration;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.CreateThriftStoreUseCase;
import com.edufelip.meer.domain.GuideContentSummary;
import com.edufelip.meer.domain.ReplaceStorePhotosUseCase;
import com.edufelip.meer.domain.StoreRatingView;
import com.edufelip.meer.domain.UpdateThriftStoreUseCase;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.dto.CategoryDto;
import com.edufelip.meer.dto.DashboardCommentDto;
import com.edufelip.meer.dto.GuideContentCommentDto;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.ImageModerationDto;
import com.edufelip.meer.dto.PhotoRegisterRequest;
import com.edufelip.meer.dto.PhotoUploadResponse;
import com.edufelip.meer.dto.PhotoUploadSlot;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.dto.StoreDtoCalculations;
import com.edufelip.meer.dto.StoreImageDto;
import com.edufelip.meer.dto.StoreRatingDto;
import com.edufelip.meer.dto.StoreRequest;
import com.edufelip.meer.dto.ThriftStoreDto;
import java.util.List;
import java.util.UUID;

public class Mappers {
  public static CategoryDto toDto(Category category) {
    return new CategoryDto(
        category.getId(),
        category.getNameStringId(),
        category.getImageResId(),
        category.getCreatedAt());
  }

  public static GuideContentDto toDto(GuideContent content) {
    return toDto(content, 0L, 0L, false);
  }

  public static GuideContentDto toDto(GuideContentSummary summary) {
    if (summary == null) return null;
    return new GuideContentDto(
        summary.id(),
        summary.title(),
        summary.description(),
        summary.imageUrl(),
        summary.thriftStoreId(),
        summary.thriftStoreId() == null ? "Guia Brechó" : summary.thriftStoreName(),
        summary.thriftStoreCoverImageUrl(),
        summary.createdAt(),
        0L,
        0L,
        false);
  }

  public static GuideContentDto toDto(
      GuideContent content, Long likeCount, Long commentCount, Boolean likedByMe) {
    return new GuideContentDto(
        content.getId(),
        content.getTitle(),
        content.getDescription(),
        content.getImageUrl(),
        content.getThriftStore() != null ? content.getThriftStore().getId() : null,
        content.getThriftStore() != null ? content.getThriftStore().getName() : "Guia Brechó",
        content.getThriftStore() != null ? content.getThriftStore().getCoverImageUrl() : null,
        content.getCreatedAt(),
        likeCount != null ? likeCount : 0L,
        commentCount != null ? commentCount : 0L,
        likedByMe != null ? likedByMe : false);
  }

  public static GuideContentDto withCounts(
      GuideContentDto base, Long likeCount, Long commentCount, Boolean likedByMe) {
    return new GuideContentDto(
        base.id(),
        base.title(),
        base.description(),
        base.imageUrl(),
        base.thriftStoreId(),
        base.thriftStoreName(),
        base.thriftStoreCoverImageUrl(),
        base.createdAt(),
        likeCount != null ? likeCount : 0L,
        commentCount != null ? commentCount : 0L,
        likedByMe != null ? likedByMe : false);
  }

  public static StoreRatingDto toDto(StoreRatingView view) {
    if (view == null) return null;
    return new StoreRatingDto(
        view.id(),
        view.storeId(),
        view.score(),
        view.body(),
        view.authorName(),
        view.authorAvatarUrl(),
        view.createdAt());
  }

  public static GuideContentCommentDto toDto(GuideContentComment comment) {
    return new GuideContentCommentDto(
        comment.getId(),
        comment.getBody(),
        comment.getUser() != null ? comment.getUser().getId() : null,
        comment.getUser() != null ? comment.getUser().getDisplayName() : null,
        comment.getUser() != null ? comment.getUser().getPhotoUrl() : null,
        comment.getCreatedAt(),
        comment.getEditedAt() != null);
  }

  public static DashboardCommentDto toDashboardCommentDto(GuideContentComment comment) {
    GuideContent content = comment.getContent();
    ThriftStore store = content != null ? content.getThriftStore() : null;
    return new DashboardCommentDto(
        comment.getId(),
        comment.getBody(),
        comment.getUser() != null ? comment.getUser().getId() : null,
        comment.getUser() != null ? comment.getUser().getDisplayName() : null,
        comment.getUser() != null ? comment.getUser().getPhotoUrl() : null,
        content != null ? content.getId() : null,
        content != null ? content.getTitle() : null,
        store != null ? store.getId() : null,
        store != null ? store.getName() : null,
        comment.getCreatedAt(),
        comment.getEditedAt() != null);
  }

  public static ThriftStoreDto toDto(ThriftStore store, boolean includeContents) {
    return toDto(store, includeContents, null, null, null, null, null);
  }

  public static ThriftStoreDto toDtoForUser(
      ThriftStore store, AuthUser user, boolean includeContents) {
    return toDto(
        store,
        includeContents,
        isFavorite(user, store != null ? store.getId() : null),
        null,
        null,
        null,
        null,
        null);
  }

  public static ThriftStoreDto toDto(
      ThriftStore store, boolean includeContents, Boolean isFavoriteOverride) {
    return toDto(store, includeContents, isFavoriteOverride, null, null, null, null);
  }

  public static ThriftStoreDto toDto(
      ThriftStore store,
      boolean includeContents,
      Boolean isFavoriteOverride,
      Double rating,
      Integer reviewCount,
      Double distanceMeters) {
    return toDto(
        store, includeContents, isFavoriteOverride, rating, reviewCount, distanceMeters, null);
  }

  public static ThriftStoreDto toDto(
      ThriftStore store,
      boolean includeContents,
      Boolean isFavoriteOverride,
      Double rating,
      Integer reviewCount,
      Double distanceMeters,
      Integer myRating) {
    return toDto(
        store,
        includeContents,
        isFavoriteOverride,
        rating,
        reviewCount,
        distanceMeters,
        myRating,
        null);
  }

  public static ThriftStoreDto toDto(
      ThriftStore store,
      boolean includeContents,
      Boolean isFavoriteOverride,
      Double rating,
      Integer reviewCount,
      Double distanceMeters,
      Integer myRating,
      List<GuideContentDto> contentsOverride) {
    List<GuideContentDto> contentsDto =
        contentsOverride != null
            ? contentsOverride
            : (includeContents && store.getContents() != null
                ? store.getContents().stream().map(Mappers::toDto).toList()
                : null);
    List<StoreImageDto> images =
        store.getPhotos() != null
            ? store.getPhotos().stream()
                .map(
                    p ->
                        new StoreImageDto(
                            p.getId(),
                            p.getUrl(),
                            p.getDisplayOrder(),
                            p.getDisplayOrder() != null && p.getDisplayOrder() == 0))
                .toList()
            : List.of();
    Integer derivedWalkMinutes =
        distanceMeters != null
            ? (int) Math.round(distanceMeters / 80.0)
            : null; // 80 m/min ≈ 4.8 km/h

    String addressToDisplay = StoreDtoCalculations.maskedAddressLine(store);

    return new ThriftStoreDto(
        store.getId(),
        store.getName(),
        store.getTagline(),
        images.isEmpty() ? store.getCoverImageUrl() : images.get(0).url(),
        addressToDisplay,
        store.getLatitude(),
        store.getLongitude(),
        store.getOpeningHours(),
        store.getSocial() != null ? store.getSocial().getFacebook() : null,
        store.getSocial() != null ? store.getSocial().getInstagram() : null,
        store.getSocial() != null ? store.getSocial().getWebsite() : null,
        store.getPhone(),
        store.getSocial() != null ? store.getSocial().getWhatsapp() : null,
        store.getCategories(),
        rating,
        reviewCount,
        myRating,
        distanceMeters,
        derivedWalkMinutes,
        store.getNeighborhood(),
        store.getBadgeLabel(),
        isFavoriteOverride,
        store.getIsOnlineStore(),
        store.getDescription(),
        contentsDto,
        images,
        store.getCreatedAt());
  }

  public static ProfileDto toProfileDto(AuthUser user, boolean includeOwnedStore) {
    ThriftStoreDto owned = null;
    if (includeOwnedStore && user.getOwnedThriftStore() != null) {
      owned = toDto(user.getOwnedThriftStore(), false);
    }
    return new ProfileDto(
        user.getId(),
        user.getDisplayName(),
        user.getEmail(),
        user.getPhotoUrl(),
        user.getBio(),
        (user.getRole() != null ? user.getRole() : Role.USER).name(),
        user.isNotifyNewStores(),
        user.isNotifyPromos(),
        owned,
        user.getCreatedAt());
  }

  public static CreateThriftStoreUseCase.Command toCreateCommand(StoreRequest body) {
    if (body == null) return null;
    return new CreateThriftStoreUseCase.Command(
        body.getName(),
        body.getDescription(),
        body.getOpeningHours(),
        body.getAddressLine(),
        body.getLatitude(),
        body.getLongitude(),
        body.getPhone(),
        body.getEmail(),
        body.getTagline(),
        body.getNeighborhood(),
        body.getIsOnlineStore(),
        body.getCategories(),
        body.getSocial() != null
            ? new CreateThriftStoreUseCase.SocialInput(
                body.getSocial().getFacebook(),
                body.getSocial().getInstagram(),
                body.getSocial().getWebsite(),
                body.getSocial().getWhatsapp())
            : null);
  }

  public static UpdateThriftStoreUseCase.Command toUpdateCommand(StoreRequest body) {
    if (body == null) return null;
    return new UpdateThriftStoreUseCase.Command(
        body.getName(),
        body.getDescription(),
        body.getOpeningHours(),
        body.getAddressLine(),
        body.getLatitude(),
        body.getLongitude(),
        body.getPhone(),
        body.getEmail(),
        body.getTagline(),
        body.getNeighborhood(),
        body.getIsOnlineStore(),
        body.getCategories(),
        body.getSocial() != null
            ? new UpdateThriftStoreUseCase.SocialUpdate(
                body.getSocial().getFacebook(),
                body.getSocial().isFacebookPresent(),
                body.getSocial().getInstagram(),
                body.getSocial().isInstagramPresent(),
                body.getSocial().getWebsite(),
                body.getSocial().isWebsitePresent(),
                body.getSocial().getWhatsapp(),
                body.getSocial().isWhatsappPresent())
            : null);
  }

  public static ReplaceStorePhotosUseCase.Command toReplacePhotosCommand(
      PhotoRegisterRequest request) {
    if (request == null) return new ReplaceStorePhotosUseCase.Command(null, null);
    List<ReplaceStorePhotosUseCase.PhotoItem> items =
        request.getPhotos() != null
            ? request.getPhotos().stream()
                .map(
                    item ->
                        new ReplaceStorePhotosUseCase.PhotoItem(
                            item.getPhotoId(), item.getFileKey(), item.getPosition()))
                .toList()
            : null;
    return new ReplaceStorePhotosUseCase.Command(items, request.getDeletePhotoIds());
  }

  public static PhotoUploadResponse toPhotoUploadResponse(List<PhotoStoragePort.UploadSlot> slots) {
    List<PhotoUploadSlot> uploads =
        slots != null
            ? slots.stream()
                .map(
                    slot ->
                        new PhotoUploadSlot(slot.uploadUrl(), slot.fileKey(), slot.contentType()))
                .toList()
            : List.of();
    return new PhotoUploadResponse(uploads);
  }

  public static Boolean isFavorite(AuthUser user, UUID storeId) {
    if (user == null || storeId == null || user.getFavorites() == null) return false;
    return user.getFavorites().stream().anyMatch(f -> storeId.equals(f.getId()));
  }

  public static ImageModerationDto toDto(ImageModeration moderation) {
    if (moderation == null) return null;
    return new ImageModerationDto(
        moderation.getId(),
        moderation.getImageUrl(),
        moderation.getStatus(),
        moderation.getEntityType(),
        moderation.getEntityId(),
        moderation.getNsfwScore(),
        moderation.getFailureReason(),
        moderation.getCreatedAt(),
        moderation.getProcessedAt(),
        moderation.getReviewedAt(),
        moderation.getReviewedBy(),
        moderation.getReviewNotes(),
        moderation.getRetryCount());
  }
}
