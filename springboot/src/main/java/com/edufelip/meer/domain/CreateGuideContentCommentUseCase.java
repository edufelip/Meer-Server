package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.util.StringSanitizer;
import java.time.Clock;
import java.time.Instant;

public class CreateGuideContentCommentUseCase {
  private static final int MAX_COMMENT_LENGTH = 120;

  private final GuideContentCommentRepository guideContentCommentRepository;
  private final GuideContentRepository guideContentRepository;
  private final Clock clock;

  public CreateGuideContentCommentUseCase(
      GuideContentCommentRepository guideContentCommentRepository,
      GuideContentRepository guideContentRepository,
      Clock clock) {
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.guideContentRepository = guideContentRepository;
    this.clock = clock;
  }

  public GuideContentComment execute(AuthUser user, GuideContent content, String body) {
    String sanitized = StringSanitizer.sanitize(body);
    if (sanitized == null || sanitized.isBlank()) {
      throw new IllegalArgumentException("comment body is required");
    }
    if (sanitized.length() > MAX_COMMENT_LENGTH) {
      throw new IllegalArgumentException("comment must be 120 characters or less");
    }
    GuideContentComment comment = new GuideContentComment(user, content, sanitized);
    Instant now = Instant.now(clock);
    comment.setCreatedAt(now);
    comment.setUpdatedAt(now);
    GuideContentComment saved = guideContentCommentRepository.save(comment);
    guideContentRepository.incrementCommentCount(content.getId());
    return saved;
  }
}
