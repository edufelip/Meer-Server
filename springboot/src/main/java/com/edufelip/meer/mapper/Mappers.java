package com.edufelip.meer.mapper;

import com.edufelip.meer.core.category.Category;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.dto.CategoryDto;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.dto.StoreImageDto;
import com.edufelip.meer.dto.ThriftStoreDto;

import java.util.List;

public class Mappers {
    public static CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getNameStringId(), category.getImageResId());
    }

    public static GuideContentDto toDto(GuideContent content) {
        return new GuideContentDto(
                content.getId(),
                content.getTitle(),
                content.getDescription(),
                content.getImageUrl(),
                content.getThriftStore() != null ? content.getThriftStore().getId() : null,
                content.getThriftStore() != null ? content.getThriftStore().getName() : null,
                content.getCreatedAt()
        );
    }

    public static ThriftStoreDto toDto(ThriftStore store, boolean includeContents) {
        return toDto(store, includeContents, store.getIsFavorite(), null, null, null);
    }

    public static ThriftStoreDto toDto(ThriftStore store, boolean includeContents, Boolean isFavoriteOverride) {
        return toDto(store, includeContents, isFavoriteOverride, null, null, null);
    }

    public static ThriftStoreDto toDto(ThriftStore store, boolean includeContents, Boolean isFavoriteOverride, Double rating, Integer reviewCount, Double distanceMeters) {
        List<GuideContentDto> contentsDto = includeContents && store.getContents() != null
                ? store.getContents().stream().map(Mappers::toDto).toList()
                : null;
        List<StoreImageDto> images = store.getPhotos() != null
                ? store.getPhotos().stream()
                .map(p -> new StoreImageDto(p.getId(), p.getUrl(), p.getDisplayOrder(), p.getDisplayOrder() != null && p.getDisplayOrder() == 0))
                .toList()
                : List.of();
        Integer derivedWalkMinutes = distanceMeters != null ? (int) Math.round(distanceMeters / 80.0) : null; // 80 m/min ≈ 4.8 km/h

        return new ThriftStoreDto(
                store.getId(),
                store.getName(),
                store.getTagline(),
                images.isEmpty() ? store.getCoverImageUrl() : images.get(0).url(),
                store.getAddressLine(),
                store.getLatitude(),
                store.getLongitude(),
                store.getOpeningHours(),
                store.getSocial() != null ? store.getSocial().getFacebook() : null,
                store.getSocial() != null ? store.getSocial().getInstagram() : null,
                store.getSocial() != null ? store.getSocial().getWebsite() : null,
                store.getPhone(),
                null,
                store.getCategories(),
                rating,
                reviewCount,
                distanceMeters,
                derivedWalkMinutes,
                store.getNeighborhood(),
                store.getBadgeLabel(),
                isFavoriteOverride,
                store.getDescription(),
                contentsDto,
                images
        );
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
                user.isNotifyNewStores(),
                user.isNotifyPromos(),
                owned
        );
    }
}
