package com.edufelip.meer.domain;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentLike;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnlikeGuideContentUseCaseTest {

  @Test
  void deletesLikeAndDecrements() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    GuideContentLikeRepository likeRepository = Mockito.mock(GuideContentLikeRepository.class);
    RateLimitPort rateLimitPort = Mockito.mock(RateLimitPort.class);
    UnlikeGuideContentUseCase useCase =
        new UnlikeGuideContentUseCase(repository, likeRepository, rateLimitPort);

    GuideContent content = new GuideContent();
    content.setId(10);
    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    when(rateLimitPort.allowLikeAction(user.getId().toString())).thenReturn(true);

    GuideContentLike like = new GuideContentLike();
    when(likeRepository.findByUserIdAndContentId(user.getId(), 10)).thenReturn(Optional.of(like));

    useCase.execute(user, 10);

    verify(likeRepository).delete(like);
    verify(repository).decrementLikeCount(10);
  }
}
