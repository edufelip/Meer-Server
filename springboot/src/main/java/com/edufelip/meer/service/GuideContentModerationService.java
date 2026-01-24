package com.edufelip.meer.service;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.cache.annotation.CacheEvict;
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
  @CacheEvict(cacheNames = "guideTop10", allEntries = true)
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
  @CacheEvict(cacheNames = "guideTop10", allEntries = true)
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
  public void hardDeleteComment(GuideContentComment comment) {
    guideContentCommentRepository.delete(comment);
    if (comment.getContent() != null) {
      guideContentRepository.decrementCommentCount(comment.getContent().getId());
    }
  }
}
