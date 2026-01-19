package com.edufelip.meer.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.PasswordResetToken;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.content.GuideContentComment;
import com.edufelip.meer.core.content.GuideContentLike;
import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushPlatform;
import com.edufelip.meer.core.push.PushToken;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.PasswordResetTokenRepository;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.support.TestFixtures;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeleteUserUseCaseTest {

  @Autowired private DeleteUserUseCase deleteUserUseCase;
  @Autowired private AuthUserRepository authUserRepository;
  @Autowired private ThriftStoreRepository thriftStoreRepository;
  @Autowired private GuideContentRepository guideContentRepository;
  @Autowired private GuideContentLikeRepository guideContentLikeRepository;
  @Autowired private GuideContentCommentRepository guideContentCommentRepository;
  @Autowired private StoreFeedbackRepository storeFeedbackRepository;
  @Autowired private PushTokenRepository pushTokenRepository;
  @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void deletesUserAndCleansRelatedData() {
    AuthUser user = authUserRepository.save(TestFixtures.user("owner@example.com", "Owner"));
    AuthUser otherUser = authUserRepository.save(TestFixtures.user("other@example.com", "Other"));

    ThriftStore ownedStore = TestFixtures.store("Owned Store");
    ownedStore.setOwner(user);
    thriftStoreRepository.save(ownedStore);
    user.setOwnedThriftStore(ownedStore);
    authUserRepository.save(user);

    ThriftStore favoriteStore = TestFixtures.store("Favorite Store");
    favoriteStore.setOwner(otherUser);
    thriftStoreRepository.save(favoriteStore);
    user.getFavorites().add(favoriteStore);
    authUserRepository.save(user);

    GuideContent ownedContent =
        new GuideContent(
            null,
            "Owned Content",
            "Owned description",
            "general",
            "TIP",
            "/uploads/owned.jpg",
            ownedStore);
    guideContentRepository.save(ownedContent);
    guideContentLikeRepository.save(new GuideContentLike(otherUser, ownedContent));
    guideContentCommentRepository.save(new GuideContentComment(otherUser, ownedContent, "Nice"));

    GuideContent otherContent =
        new GuideContent(
            null,
            "Other Content",
            "Other description",
            "general",
            "TIP",
            "/uploads/other.jpg",
            favoriteStore);
    otherContent.setDeletedBy(user);
    otherContent.setDeletedAt(Instant.now());
    guideContentRepository.save(otherContent);

    GuideContentComment editedComment =
        new GuideContentComment(otherUser, otherContent, "Edited comment");
    editedComment.setEditedBy(user);
    guideContentCommentRepository.save(editedComment);
    guideContentLikeRepository.save(new GuideContentLike(user, otherContent));

    storeFeedbackRepository.save(TestFixtures.feedback(user, favoriteStore, 5, "Great"));

    PushToken pushToken = new PushToken();
    pushToken.setUserId(user.getId());
    pushToken.setDeviceId("device-1");
    pushToken.setFcmToken("token");
    pushToken.setPlatform(PushPlatform.IOS);
    pushToken.setEnvironment(PushEnvironment.DEV);
    pushToken.setLastSeenAt(Instant.now());
    pushTokenRepository.save(pushToken);

    passwordResetTokenRepository.save(
        new PasswordResetToken(UUID.randomUUID(), user, Instant.now().plusSeconds(3600)));

    entityManager.flush();
    entityManager.clear();

    deleteUserUseCase.execute(user, "ACCOUNT_DELETE");

    entityManager.flush();
    entityManager.clear();

    assertThat(authUserRepository.findById(user.getId())).isEmpty();
    assertThat(thriftStoreRepository.findById(ownedStore.getId())).isEmpty();
    assertThat(guideContentRepository.findById(ownedContent.getId())).isEmpty();
    assertThat(guideContentLikeRepository.findAll()).isEmpty();
    assertThat(storeFeedbackRepository.findAll()).isEmpty();
    assertThat(pushTokenRepository.findByUserIdIn(List.of(user.getId()))).isEmpty();
    assertThat(passwordResetTokenRepository.findAll()).isEmpty();

    GuideContent remainingContent =
        guideContentRepository.findById(otherContent.getId()).orElseThrow();
    assertThat(remainingContent.getDeletedBy()).isNull();

    List<GuideContentComment> remainingComments = guideContentCommentRepository.findAll();
    assertThat(remainingComments).hasSize(1);
    GuideContentComment remainingComment = remainingComments.get(0);
    assertThat(remainingComment.getId()).isEqualTo(editedComment.getId());
    assertThat(remainingComment.getEditedBy()).isNull();
  }
}
