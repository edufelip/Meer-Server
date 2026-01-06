package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.util.StringSanitizer;
import java.time.Clock;
import java.time.Instant;

public class UpdateGuideContentCommentUseCase {
  private static final int MAX_COMMENT_LENGTH = 120;

  private final GuideContentCommentRepository guideContentCommentRepository;
  private final Clock clock;

  public UpdateGuideContentCommentUseCase(
      GuideContentCommentRepository guideContentCommentRepository, Clock clock) {
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.clock = clock;
  }

  public GuideContentComment execute(GuideContentComment comment, String body, AuthUser editor) {
    String sanitized = StringSanitizer.sanitize(body);
    if (sanitized == null || sanitized.isBlank()) {
      throw new IllegalArgumentException("comment body is required");
    }
    if (sanitized.length() > MAX_COMMENT_LENGTH) {
      throw new IllegalArgumentException("comment must be 120 characters or less");
    }
    if (sanitized.equals(comment.getBody())) {
      return comment;
    }
    comment.setBody(sanitized);
    Instant now = Instant.now(clock);
    comment.setUpdatedAt(now);
    comment.setEditedAt(now);
    comment.setEditedBy(editor);
    return guideContentCommentRepository.save(comment);
  }
}
