package com.edufelip.meer.mapper;

import com.edufelip.meer.core.category.Category;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.dto.CategoryDto;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.ThriftStoreDto;

import java.util.List;

public class Mappers {
    public static CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getImageUrl());
    }

    public static GuideContentDto toDto(GuideContent content) {
        return new GuideContentDto(
                content.getId(),
                content.getTitle(),
                content.getDescription(),
                content.getCategoryLabel(),
                content.getImageUrl(),
                content.getThriftStore() != null ? content.getThriftStore().getId() : null
        );
    }

    public static ThriftStoreDto toDto(ThriftStore store, boolean includeContents) {
        List<GuideContentDto> contentsDto = includeContents && store.getContents() != null
                ? store.getContents().stream().map(Mappers::toDto).toList()
                : null;
        return new ThriftStoreDto(
                store.getId(),
                store.getName(),
                store.getTagline(),
                store.getCoverImageUrl(),
                store.getGalleryUrls(),
                store.getAddressLine(),
                store.getLatitude(),
                store.getLongitude(),
                store.getMapImageUrl(),
                store.getOpeningHours(),
                store.getOpeningHoursNotes(),
                store.getSocial() != null ? store.getSocial().getFacebook() : null,
                store.getSocial() != null ? store.getSocial().getInstagram() : null,
                store.getSocial() != null ? store.getSocial().getWebsite() : null,
                store.getSocial() != null ? store.getSocial().getWhatsapp() : null,
                store.getCategories(),
                store.getDistanceKm(),
                store.getWalkTimeMinutes(),
                store.getNeighborhood(),
                store.getBadgeLabel(),
                store.getIsFavorite(),
                store.getDescription(),
                contentsDto
        );
    }
}
