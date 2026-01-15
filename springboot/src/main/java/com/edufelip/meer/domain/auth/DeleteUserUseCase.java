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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class DeleteUserUseCase {
  private static final Logger log = LoggerFactory.getLogger(DeleteUserUseCase.class);

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
  private final JdbcTemplate jdbcTemplate;
  private Boolean commentDeletedByColumn;

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
      PasswordResetTokenRepository passwordResetTokenRepository,
      JdbcTemplate jdbcTemplate) {
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
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public void execute(AuthUser user, String sourceType) {
    if (user == null || user.getId() == null) return;
    AuthUser managedUser = authUserRepository.findById(user.getId()).orElse(null);
    if (managedUser == null) return;
    Set<UUID> processed = new HashSet<>();

    log.info("Deleting user account userId={} sourceType={}", managedUser.getId(), sourceType);

    Optional.ofNullable(managedUser.getOwnedThriftStore())
        .flatMap(store -> thriftStoreRepository.findById(store.getId()))
        .ifPresent(
            store -> storeDeletionService.deleteStoreWithAssets(store, processed, sourceType));

    thriftStoreRepository
        .findByOwnerId(managedUser.getId())
        .forEach(store -> storeDeletionService.deleteStoreWithAssets(store, processed, sourceType));

    if (managedUser.getPhotoUrl() != null) {
      assetDeletionQueuePort.enqueueAll(
          java.util.List.of(managedUser.getPhotoUrl()),
          "USER_AVATAR_DELETE",
          managedUser.getId().toString());
    }

    guideContentCommentRepository.clearEditedByUserId(managedUser.getId());
    if (hasCommentDeletedByColumn()) {
      guideContentCommentRepository.clearDeletedByUserId(managedUser.getId());
    }
    guideContentRepository.clearDeletedByUserId(managedUser.getId());
    guideContentCommentRepository.deleteByUserId(managedUser.getId());
    guideContentLikeRepository.deleteByUserId(managedUser.getId());
    pushTokenRepository.deleteByUserId(managedUser.getId());
    passwordResetTokenRepository.deleteByUserId(managedUser.getId());
    authUserRepository.deleteFavoritesByUserId(managedUser.getId());

    storeFeedbackRepository.deleteByUserId(managedUser.getId());
    authUserRepository.delete(managedUser);
  }

  private boolean hasCommentDeletedByColumn() {
    if (commentDeletedByColumn != null) {
      return commentDeletedByColumn;
    }
    try {
      Integer count =
          jdbcTemplate.queryForObject(
              """
                  select count(*)
                  from information_schema.columns
                  where upper(table_schema) = 'PUBLIC'
                    and upper(table_name) = 'GUIDE_CONTENT_COMMENT'
                    and upper(column_name) = 'DELETED_BY_USER_ID'
                  """,
              Integer.class);
      commentDeletedByColumn = count != null && count > 0;
      return commentDeletedByColumn;
    } catch (Exception ex) {
      log.warn("Failed to inspect guide_content_comment.deleted_by_user_id column", ex);
      commentDeletedByColumn = false;
      return false;
    }
  }
}
