package com.edufelip.meer.core.moderation;

public enum ModerationStatus {
  /** Image is queued for processing */
  PENDING,

  /** Image is currently being processed */
  PROCESSING,

  /** Image passed automated checks (safe content) */
  APPROVED,

  /** Image flagged for manual review (score in gray zone) */
  FLAGGED_FOR_REVIEW,

  /** Image blocked by automated system (high NSFW score) */
  BLOCKED,

  /** Image approved after manual review */
  MANUALLY_APPROVED,

  /** Image rejected after manual review */
  MANUALLY_REJECTED,

  /** Processing failed (technical error) */
  FAILED
}
