package com.edufelip.meer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AcceptTermsRequest(
    @JsonProperty("terms_version") @NotBlank(message = "terms_version is required")
        String termsVersion) {}
