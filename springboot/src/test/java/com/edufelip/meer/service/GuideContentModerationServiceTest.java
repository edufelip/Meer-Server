package com.edufelip.meer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.domain.port.AssetDeletionQueuePort;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.support.TestFixtures;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GuideContentModerationServiceTest {

  @Test
  void hardDeleteCommentRemovesAndDecrementsCount() {
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    AssetDeletionQueuePort assetDeletionQueuePort = Mockito.mock(AssetDeletionQueuePort.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    GuideContentModerationService service =
        new GuideContentModerationService(contentRepository, commentRepository, assetDeletionQueuePort, clock);

    AuthUser actor = new AuthUser();
    actor.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(5);

    GuideContentComment comment = new GuideContentComment(actor, content, "Hello");
    comment.setId(9);

    service.hardDeleteComment(comment);

    Mockito.verify(commentRepository).delete(comment);
    Mockito.verify(contentRepository).decrementCommentCount(5);
  }

  @Test
  void softDeleteAndRestoreContent() {
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    AssetDeletionQueuePort assetDeletionQueuePort = Mockito.mock(AssetDeletionQueuePort.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    GuideContentModerationService service =
        new GuideContentModerationService(contentRepository, commentRepository, assetDeletionQueuePort, clock);

    AuthUser actor = new AuthUser();
    actor.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(7);

    Mockito.when(contentRepository.save(Mockito.any(GuideContent.class)))
        .thenAnswer(inv -> inv.getArgument(0, GuideContent.class));

    GuideContent deleted = service.softDeleteContent(content, actor, "spam");
    assertThat(deleted.getDeletedAt()).isEqualTo(TestFixtures.fixedInstant());
    assertThat(deleted.getDeletedBy()).isEqualTo(actor);
    assertThat(deleted.getDeletedReason()).isEqualTo("spam");

    GuideContent restored = service.restoreContent(content);
    assertThat(restored.getDeletedAt()).isNull();
    assertThat(restored.getDeletedBy()).isNull();
    assertThat(restored.getDeletedReason()).isNull();
  }

  @Test
  void softDeleteContentEnqueuesImageForDeletion() {
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    AssetDeletionQueuePort assetDeletionQueuePort = Mockito.mock(AssetDeletionQueuePort.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    GuideContentModerationService service =
        new GuideContentModerationService(contentRepository, commentRepository, assetDeletionQueuePort, clock);

    AuthUser actor = new AuthUser();
    actor.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setImageUrl("https://storage.googleapis.com/bucket/contents/abc-123");

    Mockito.when(contentRepository.save(Mockito.any(GuideContent.class)))
        .thenAnswer(inv -> inv.getArgument(0, GuideContent.class));

    service.softDeleteContent(content, actor, "inappropriate");

    Mockito.verify(assetDeletionQueuePort).enqueueAll(
        List.of("https://storage.googleapis.com/bucket/contents/abc-123"),
        "GUIDE_CONTENT_DELETE",
        "10");
  }

  @Test
  void softDeleteContentWithoutImageDoesNotEnqueue() {
    GuideContentRepository contentRepository = Mockito.mock(GuideContentRepository.class);
    GuideContentCommentRepository commentRepository =
        Mockito.mock(GuideContentCommentRepository.class);
    AssetDeletionQueuePort assetDeletionQueuePort = Mockito.mock(AssetDeletionQueuePort.class);
    Clock clock = Clock.fixed(TestFixtures.fixedInstant(), ZoneOffset.UTC);
    GuideContentModerationService service =
        new GuideContentModerationService(contentRepository, commentRepository, assetDeletionQueuePort, clock);

    AuthUser actor = new AuthUser();
    actor.setId(UUID.randomUUID());

    GuideContent content = new GuideContent();
    content.setId(11);
    // No imageUrl set

    Mockito.when(contentRepository.save(Mockito.any(GuideContent.class)))
        .thenAnswer(inv -> inv.getArgument(0, GuideContent.class));

    service.softDeleteContent(content, actor, "spam");

    Mockito.verify(assetDeletionQueuePort, Mockito.never()).enqueueAll(
        Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
  }
}
