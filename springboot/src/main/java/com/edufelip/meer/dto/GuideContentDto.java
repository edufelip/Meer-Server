package com.edufelip.meer.dto;

public record GuideContentDto(
        Integer id,
        String title,
        String description,
        String imageUrl,
        java.util.UUID thriftStoreId,
        String thriftStoreName,
        java.time.Instant createdAt
) {}
