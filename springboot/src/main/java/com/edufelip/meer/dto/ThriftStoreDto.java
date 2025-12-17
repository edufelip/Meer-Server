package com.edufelip.meer.dto;

import java.util.List;
import java.time.Instant;

public record ThriftStoreDto(
        java.util.UUID id,
        String name,
        String tagline,
        String coverImageUrl,
        String addressLine,
        Double latitude,
        Double longitude,
        String openingHours,
        String facebook,
        String instagram,
        String website,
        String phone,
        String whatsapp,
        List<String> categories,
        Double rating,
        Integer reviewCount,
        Integer myRating,
        Double distanceMeters,
        Integer walkTimeMinutes,
        String neighborhood,
        String badgeLabel,
        Boolean isFavorite,
        String description,
        List<GuideContentDto> contents,
        List<StoreImageDto> images,
        Instant createdAt
) {}
