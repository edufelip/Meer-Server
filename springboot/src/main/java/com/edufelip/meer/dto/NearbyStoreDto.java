package com.edufelip.meer.dto;

import java.util.List;

public record NearbyStoreDto(
        java.util.UUID id,
        String name,
        String description,
        String coverImageUrl,
        String addressLine,
        Double latitude,
        Double longitude,
        String neighborhood,
        Boolean isFavorite,
        List<String> categories,
        Double rating,
        Integer reviewCount,
        Double distanceMeters,
        Integer walkTimeMinutes
) {}
