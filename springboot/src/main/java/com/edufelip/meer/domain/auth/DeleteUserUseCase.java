package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.StoreDeletionService;
import com.edufelip.meer.domain.port.AssetDeletionQueuePort;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.PasswordResetTokenRepository;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DeleteUserUseCase {
  private final AuthUserRepository authUserRepository;
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreFeedbackRepository storeFeedbackRepository;
  private final StoreDeletionService storeDeletionService;
  private final AssetDeletionQueuePort assetDeletionQueuePort;
  private final GuideContentCommentRepository guideContentCommentRepository;
  private final GuideContentLikeRepository guideContentLikeRepository;
  private final GuideContentRepository guideContentRepository;
  private final PushTokenRepository pushTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;

  public DeleteUserUseCase(
      AuthUserRepository authUserRepository,
      ThriftStoreRepository thriftStoreRepository,
      StoreFeedbackRepository storeFeedbackRepository,
      StoreDeletionService storeDeletionService,
      AssetDeletionQueuePort assetDeletionQueuePort,
      GuideContentCommentRepository guideContentCommentRepository,
      GuideContentLikeRepository guideContentLikeRepository,
      GuideContentRepository guideContentRepository,
      PushTokenRepository pushTokenRepository,
      PasswordResetTokenRepository passwordResetTokenRepository) {
    this.authUserRepository = authUserRepository;
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeFeedbackRepository = storeFeedbackRepository;
    this.storeDeletionService = storeDeletionService;
    this.assetDeletionQueuePort = assetDeletionQueuePort;
    this.guideContentCommentRepository = guideContentCommentRepository;
    this.guideContentLikeRepository = guideContentLikeRepository;
    this.guideContentRepository = guideContentRepository;
    this.pushTokenRepository = pushTokenRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
  }

  @Transactional
  public void execute(AuthUser user, String sourceType) {
    if (user == null) return;
    Set<UUID> processed = new HashSet<>();

    Optional.ofNullable(user.getOwnedThriftStore())
        .flatMap(store -> thriftStoreRepository.findById(store.getId()))
        .ifPresent(
            store -> storeDeletionService.deleteStoreWithAssets(store, processed, sourceType));

    thriftStoreRepository
        .findByOwnerId(user.getId())
        .forEach(store -> storeDeletionService.deleteStoreWithAssets(store, processed, sourceType));

    if (user.getPhotoUrl() != null) {
      assetDeletionQueuePort.enqueueAll(
          java.util.List.of(user.getPhotoUrl()), "USER_AVATAR_DELETE", user.getId().toString());
    }

    guideContentCommentRepository.clearEditedByUserId(user.getId());
    guideContentRepository.clearDeletedByUserId(user.getId());
    guideContentCommentRepository.deleteByUserId(user.getId());
    guideContentLikeRepository.deleteByUserId(user.getId());
    pushTokenRepository.deleteByUserId(user.getId());
    passwordResetTokenRepository.deleteByUserId(user.getId());

    user.getFavorites().clear();
    authUserRepository.save(user);

    storeFeedbackRepository.deleteByUserId(user.getId());
    authUserRepository.delete(user);
  }
}
