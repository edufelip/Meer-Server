package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LikeGuideContentUseCaseTest {

  @Test
  void createsLikeAndIncrements() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    GuideContentLikeRepository likeRepository = Mockito.mock(GuideContentLikeRepository.class);
    RateLimitPort rateLimitPort = Mockito.mock(RateLimitPort.class);
    LikeGuideContentUseCase useCase =
        new LikeGuideContentUseCase(repository, likeRepository, rateLimitPort);

    GuideContent content = new GuideContent();
    content.setId(10);
    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    when(rateLimitPort.allowLikeAction(user.getId().toString())).thenReturn(true);
    when(likeRepository.existsByUserIdAndContentId(user.getId(), 10)).thenReturn(false);

    useCase.execute(user, 10);

    verify(likeRepository).save(Mockito.any());
    verify(repository).incrementLikeCount(10);
  }

  @Test
  void rejectsWhenRateLimited() {
    GuideContentRepository repository = Mockito.mock(GuideContentRepository.class);
    GuideContentLikeRepository likeRepository = Mockito.mock(GuideContentLikeRepository.class);
    RateLimitPort rateLimitPort = Mockito.mock(RateLimitPort.class);
    LikeGuideContentUseCase useCase =
        new LikeGuideContentUseCase(repository, likeRepository, rateLimitPort);

    GuideContent content = new GuideContent();
    content.setId(10);
    when(repository.findByIdAndDeletedAtIsNull(10)).thenReturn(Optional.of(content));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    when(rateLimitPort.allowLikeAction(user.getId().toString())).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(user, 10))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

    verify(likeRepository, never()).save(Mockito.any());
  }
}
