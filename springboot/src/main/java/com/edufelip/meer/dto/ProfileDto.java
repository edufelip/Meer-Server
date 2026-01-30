package com.edufelip.meer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProfileDto(
    java.util.UUID id,
    String name,
    String email,
    @JsonProperty("terms_version") String termsVersion,
    @JsonProperty("terms_accepted_at") java.time.Instant termsAcceptedAt,
    @JsonProperty("terms_required_version") String termsRequiredVersion,
    @JsonProperty("terms_url") String termsUrl,
    String avatarUrl,
    String bio,
    String role,
    boolean notifyNewStores,
    boolean notifyPromos,
    ThriftStoreDto ownedThriftStore,
    java.time.Instant createdAt) {}
