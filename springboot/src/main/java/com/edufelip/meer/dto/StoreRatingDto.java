package com.edufelip.meer.dto;

import java.time.Instant;
import java.util.UUID;

public record StoreRatingDto(
        Integer id,
        UUID storeId,
        Integer score,
        String body,
        String authorName,
        String authorAvatarUrl,
        Instant createdAt
) {}
