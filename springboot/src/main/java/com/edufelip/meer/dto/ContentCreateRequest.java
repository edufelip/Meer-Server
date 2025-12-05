package com.edufelip.meer.dto;

public record ContentCreateRequest(
        String title,
        String description,
        java.util.UUID storeId
) {}
