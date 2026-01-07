package com.edufelip.meer.dto;

public record DashboardCommentDto(
    Integer id,
    String body,
    java.util.UUID userId,
    String userDisplayName,
    String userPhotoUrl,
    Integer contentId,
    String contentTitle,
    java.util.UUID thriftStoreId,
    String thriftStoreName,
    java.time.Instant createdAt,
    Boolean edited) {}
