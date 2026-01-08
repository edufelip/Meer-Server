package com.edufelip.meer.domain;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnlikeGuideContentUseCase {
  private final GuideContentRepository guideContentRepository;
  private final GuideContentLikeRepository guideContentLikeRepository;
  private final RateLimitPort rateLimitPort;

  public UnlikeGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      GuideContentLikeRepository guideContentLikeRepository,
      RateLimitPort rateLimitPort) {
    this.guideContentRepository = guideContentRepository;
    this.guideContentLikeRepository = guideContentLikeRepository;
    this.rateLimitPort = rateLimitPort;
  }

  public void execute(AuthUser user, Integer contentId) {
    var content =
        guideContentRepository
            .findByIdAndDeletedAtIsNull(contentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
    if (!rateLimitPort.allowLikeAction(user.getId().toString())) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many like actions");
    }
    guideContentLikeRepository
        .findByUserIdAndContentId(user.getId(), content.getId())
        .ifPresent(
            like -> {
              guideContentLikeRepository.delete(like);
              guideContentRepository.decrementLikeCount(content.getId());
            });
  }
}
