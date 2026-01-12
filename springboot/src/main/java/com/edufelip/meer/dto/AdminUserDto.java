package com.edufelip.meer.dto;

import java.time.Instant;
import java.util.List;

public record AdminUserDto(
    String id,
    String name,
    String email,
    String role,
    Instant createdAt,
    String photoUrl,
    List<PushTokenDto> pushTokens) {}
