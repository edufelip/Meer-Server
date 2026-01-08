package com.edufelip.meer.domain;

import java.time.Instant;
import java.util.UUID;

public record GuideContentSummary(
    Integer id,
    String title,
    String description,
    String imageUrl,
    UUID thriftStoreId,
    String thriftStoreName,
    String thriftStoreCoverImageUrl,
    Instant createdAt) {}
