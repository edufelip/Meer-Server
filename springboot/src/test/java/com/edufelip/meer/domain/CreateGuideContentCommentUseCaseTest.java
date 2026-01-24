package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.support.TestFixtures;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CreateGuideContentCommentUseCaseTest {

  @Test
  void sanitizesBodyAndIncrementsCount() {
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    CreateGuideContentCommentUseCase useCase =
        new CreateGuideContentCommentUseCase(commentRepository, contentRepository, clock);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(4);

    Mockito.when(commentRepository.save(Mockito.any(GuideContentComment.class)))
        .thenAnswer(inv -> inv.getArgument(0, GuideContentComment.class));

    GuideContentComment created = useCase.execute(user, content, "<b> Hello </b>");

    assertThat(created.getBody()).isEqualTo("Hello");
    assertThat(created.getCreatedAt()).isEqualTo(TestFixtures.fixedInstant());
    assertThat(created.getUpdatedAt()).isEqualTo(TestFixtures.fixedInstant());
    Mockito.verify(contentRepository).incrementCommentCount(4);
  }

  @Test
  void rejectsBlankBodyAfterSanitize() {
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    CreateGuideContentCommentUseCase useCase =
        new CreateGuideContentCommentUseCase(commentRepository, contentRepository, clock);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(4);

    assertThatThrownBy(() -> useCase.execute(user, content, "<b>   </b>"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("comment body is required");
  }

  @Test
  void rejectsTooLongBody() {
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    CreateGuideContentCommentUseCase useCase =
        new CreateGuideContentCommentUseCase(commentRepository, contentRepository, clock);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(4);

    String tooLong = "a".repeat(121);

    assertThatThrownBy(() -> useCase.execute(user, content, tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("comment must be 120 characters or less");
  }

  @Test
  void preservesNewlinesInCommentBody() {
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    CreateGuideContentCommentUseCase useCase =
        new CreateGuideContentCommentUseCase(commentRepository, contentRepository, clock);

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(4);

    Mockito.when(commentRepository.save(Mockito.any(GuideContentComment.class)))
        .thenAnswer(inv -> inv.getArgument(0, GuideContentComment.class));

    GuideContentComment created = useCase.execute(user, content, "Line 1\nLine 2\nLine 3");

    assertThat(created.getBody()).isEqualTo("Line 1\nLine 2\nLine 3");
    assertThat(created.getBody()).contains("\n");
    Mockito.verify(contentRepository).incrementCommentCount(4);
  }
}
