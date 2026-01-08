package com.edufelip.meer.domain;

import java.time.Instant;
import java.util.UUID;

public record StoreRatingView(
    Integer id,
    UUID storeId,
    Integer score,
    String body,
    String authorName,
    String authorAvatarUrl,
    Instant createdAt) {}
