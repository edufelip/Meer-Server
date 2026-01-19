package com.edufelip.meer.dto;

import com.edufelip.meer.core.moderation.EntityType;
import com.edufelip.meer.core.moderation.ModerationStatus;
import java.time.Instant;

public record ImageModerationDto(
    Long id,
    String imageUrl,
    ModerationStatus status,
    EntityType entityType,
    String entityId,
    Double nsfwScore,
    String failureReason,
    Instant createdAt,
    Instant processedAt,
    Instant reviewedAt,
    String reviewedBy,
    String reviewNotes,
    int retryCount) {}
