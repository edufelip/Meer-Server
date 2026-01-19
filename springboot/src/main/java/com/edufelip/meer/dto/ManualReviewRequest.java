package com.edufelip.meer.dto;

import com.edufelip.meer.core.moderation.ModerationStatus;

public record ManualReviewRequest(ModerationStatus decision, String notes) {}
