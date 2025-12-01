package com.edufelip.meer.dto;

import java.util.List;

public record CategoryStoreItemDto(
        java.util.UUID id,
        String name,
        String coverImage,
        String addressLine,
        Double rating,
        Integer reviewCount,
        List<String> categories,
        Boolean isFavorite
) {}
