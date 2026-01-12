package com.edufelip.meer.dto;

import java.time.Instant;

public record PushTokenDto(
    String id,
    String deviceId,
    String platform,
    String environment,
    String appVersion,
    Instant lastSeenAt,
    Instant createdAt) {}
