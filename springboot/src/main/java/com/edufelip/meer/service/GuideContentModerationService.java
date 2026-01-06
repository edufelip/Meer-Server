package com.edufelip.meer.service;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuideContentModerationService {
  private final GuideContentRepository guideContentRepository;
  private final GuideContentCommentRepository guideContentCommentRepository;
  private final Clock clock;

  public GuideContentModerationService(
      GuideContentRepository guideContentRepository,
      GuideContentCommentRepository guideContentCommentRepository,
      Clock clock) {
    this.guideContentRepository = guideContentRepository;
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.clock = clock;
  }

  @Transactional
  public GuideContent softDeleteContent(GuideContent content, AuthUser actor, String reason) {
    if (content.getDeletedAt() == null) {
      content.setDeletedAt(Instant.now(clock));
      content.setDeletedBy(actor);
      content.setDeletedReason(reason);
      return guideContentRepository.save(content);
    }
    return content;
  }

  @Transactional
  public GuideContent restoreContent(GuideContent content) {
    if (content.getDeletedAt() != null) {
      content.setDeletedAt(null);
      content.setDeletedBy(null);
      content.setDeletedReason(null);
      return guideContentRepository.save(content);
    }
    return content;
  }

  @Transactional
  public GuideContentComment softDeleteComment(
      GuideContentComment comment, AuthUser actor, String reason) {
    if (comment.getDeletedAt() == null) {
      comment.setDeletedAt(Instant.now(clock));
      comment.setDeletedBy(actor);
      comment.setDeletedReason(reason);
      GuideContentComment saved = guideContentCommentRepository.save(comment);
      if (comment.getContent() != null) {
        guideContentRepository.decrementCommentCount(comment.getContent().getId());
      }
      return saved;
    }
    return comment;
  }

  @Transactional
  public GuideContentComment restoreComment(GuideContentComment comment) {
    if (comment.getDeletedAt() != null) {
      comment.setDeletedAt(null);
      comment.setDeletedBy(null);
      comment.setDeletedReason(null);
      GuideContentComment saved = guideContentCommentRepository.save(comment);
      if (comment.getContent() != null) {
        guideContentRepository.incrementCommentCount(comment.getContent().getId());
      }
      return saved;
    }
    return comment;
  }
}
