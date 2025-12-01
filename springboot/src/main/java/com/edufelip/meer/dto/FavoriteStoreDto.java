package com.edufelip.meer.dto;

public record FavoriteStoreDto(
        java.util.UUID id,
        String name,
        String description,
        String coverImageUrl,
        Double latitude,
        Double longitude,
        Boolean isFavorite,
        Double distanceMeters
) {}
