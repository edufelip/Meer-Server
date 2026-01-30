package com.edufelip.meer.dto;

public record AdminProfileDto(
    java.util.UUID id,
    String name,
    String email,
    String avatarUrl,
    String bio,
    String role,
    boolean notifyNewStores,
    boolean notifyPromos,
    ThriftStoreDto ownedThriftStore,
    java.time.Instant createdAt) {}
