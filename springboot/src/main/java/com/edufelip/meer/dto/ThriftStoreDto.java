package com.edufelip.meer.dto;

import java.util.List;

public record ThriftStoreDto(
        Integer id,
        String name,
        String tagline,
        String coverImageUrl,
        List<String> galleryUrls,
        String addressLine,
        Double latitude,
        Double longitude,
        String mapImageUrl,
        String openingHours,
        String openingHoursNotes,
        String facebook,
        String instagram,
        String website,
        String whatsapp,
        List<String> categories,
        Double rating,
        Integer reviewCount,
        Double distanceMeters,
        Double distanceKm,
        Integer walkTimeMinutes,
        String neighborhood,
        String badgeLabel,
        Boolean isFavorite,
        String description,
        List<GuideContentDto> contents,
        List<StoreImageDto> images
) {}
