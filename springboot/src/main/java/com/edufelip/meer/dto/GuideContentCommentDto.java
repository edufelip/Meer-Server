package com.edufelip.meer.dto;

public record GuideContentCommentDto(
    Integer id,
    String body,
    java.util.UUID userId,
    String userDisplayName,
    String userPhotoUrl,
    java.time.Instant createdAt,
    Boolean edited) {}
