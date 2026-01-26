package com.edufelip.meer.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardStoreSummaryDto(
    UUID id, String name, String addressLine, Boolean isOnlineStore, Instant createdAt) {}
