package com.edufelip.meer.dto;

public record GuideTopDto(
        Integer id,
        String title,
        String description,
        String imageUrl,
        java.util.UUID thriftStoreId,
        String thriftStoreName,
        java.time.Instant createdAt
) {}
