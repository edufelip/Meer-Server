package com.edufelip.meer;

import com.edufelip.meer.config.FirebaseProperties;
import com.edufelip.meer.config.TermsProperties;
import com.edufelip.meer.domain.CreateCategoryUseCase;
import com.edufelip.meer.domain.CreateGuideContentCommentUseCase;
import com.edufelip.meer.domain.CreateGuideContentUseCase;
import com.edufelip.meer.domain.CreateOwnedGuideContentUseCase;
import com.edufelip.meer.domain.CreateThriftStoreUseCase;
import com.edufelip.meer.domain.DeleteCategoryUseCase;
import com.edufelip.meer.domain.DeleteGuideContentUseCase;
import com.edufelip.meer.domain.DeletePushTokenUseCase;
import com.edufelip.meer.domain.DeleteThriftStoreUseCase;
import com.edufelip.meer.domain.GetCategoriesUseCase;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.GetGuideContentsByThriftStoreUseCase;
import com.edufelip.meer.domain.GetStoreContentsUseCase;
import com.edufelip.meer.domain.GetStoreDetailsUseCase;
import com.edufelip.meer.domain.GetStoreListingsUseCase;
import com.edufelip.meer.domain.GetThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.LikeGuideContentUseCase;
import com.edufelip.meer.domain.ReplaceStorePhotosUseCase;
import com.edufelip.meer.domain.RequestGuideContentImageUploadUseCase;
import com.edufelip.meer.domain.RequestStorePhotoUploadsUseCase;
import com.edufelip.meer.domain.StoreDeletionService;
import com.edufelip.meer.domain.StoreOwnershipService;
import com.edufelip.meer.domain.UnlikeGuideContentUseCase;
import com.edufelip.meer.domain.UpdateCategoryUseCase;
import com.edufelip.meer.domain.UpdateGuideContentCommentUseCase;
import com.edufelip.meer.domain.UpdateGuideContentUseCase;
import com.edufelip.meer.domain.UpdateThriftStoreUseCase;
import com.edufelip.meer.domain.UpsertPushTokenUseCase;
import com.edufelip.meer.domain.auth.AppleLoginUseCase;
import com.edufelip.meer.domain.auth.AcceptTermsUseCase;
import com.edufelip.meer.domain.auth.DashboardLoginUseCase;
import com.edufelip.meer.domain.auth.DeleteUserUseCase;
import com.edufelip.meer.domain.auth.ForgotPasswordUseCase;
import com.edufelip.meer.domain.auth.GetProfileUseCase;
import com.edufelip.meer.domain.auth.GoogleLoginUseCase;
import com.edufelip.meer.domain.auth.LoginUseCase;
import com.edufelip.meer.domain.auth.PasswordResetNotifier;
import com.edufelip.meer.domain.auth.RefreshTokenUseCase;
import com.edufelip.meer.domain.auth.ResetPasswordUseCase;
import com.edufelip.meer.domain.auth.SignupUseCase;
import com.edufelip.meer.domain.auth.UpdateProfileUseCase;
import com.edufelip.meer.domain.port.AssetDeletionQueuePort;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.domain.repo.GuideContentCommentRepository;
import com.edufelip.meer.domain.repo.GuideContentLikeRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.PasswordResetTokenRepository;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.logging.RequestResponseLoggingFilter;
import com.edufelip.meer.security.DashboardAdminGuardFilter;
import com.edufelip.meer.security.GoogleClientProperties;
import com.edufelip.meer.security.JwtProperties;
import com.edufelip.meer.security.PasswordResetProperties;
import com.edufelip.meer.security.RequestGuardsFilter;
import com.edufelip.meer.security.SecurityProperties;
import com.edufelip.meer.security.token.JwtTokenProvider;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GuideContentEngagementService;
import com.edufelip.meer.service.GuideContentModerationService;
import com.edufelip.meer.service.PasswordResetTokenService;
import com.edufelip.meer.service.StoreFeedbackService;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({
  SecurityProperties.class,
  JwtProperties.class,
  GoogleClientProperties.class,
  PasswordResetProperties.class,
  FirebaseProperties.class,
  TermsProperties.class
})
public class AppConfig {

  @Bean
  public GetThriftStoreUseCase getThriftStoreUseCase(ThriftStoreRepository repo) {
    return new GetThriftStoreUseCase(repo);
  }

  @Bean
  public GetThriftStoresUseCase getThriftStoresUseCase(
      ThriftStoreRepository repo,
      @org.springframework.beans.factory.annotation.Value("${spring.datasource.url:}")
          String datasourceUrl,
      @org.springframework.beans.factory.annotation.Value("${meer.postgis.enabled:false}")
          boolean postgisEnabled) {
    return new GetThriftStoresUseCase(repo, datasourceUrl, postgisEnabled);
  }

  @Bean
  public StoreOwnershipService storeOwnershipService(
      AuthUserRepository authUserRepository, ThriftStoreRepository thriftStoreRepository) {
    return new StoreOwnershipService(authUserRepository, thriftStoreRepository);
  }

  @Bean
  public CreateThriftStoreUseCase createThriftStoreUseCase(
      ThriftStoreRepository repo, AuthUserRepository authUserRepository) {
    return new CreateThriftStoreUseCase(repo, authUserRepository);
  }

  @Bean
  public GetStoreContentsUseCase getStoreContentsUseCase(
      GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase,
      GuideContentEngagementService guideContentEngagementService) {
    return new GetStoreContentsUseCase(
        getGuideContentsByThriftStoreUseCase, guideContentEngagementService);
  }

  @Bean
  public GetStoreListingsUseCase getStoreListingsUseCase(
      GetThriftStoresUseCase getThriftStoresUseCase,
      ThriftStoreRepository thriftStoreRepository,
      StoreFeedbackService storeFeedbackService,
      CategoryRepository categoryRepository) {
    return new GetStoreListingsUseCase(
        getThriftStoresUseCase, thriftStoreRepository, storeFeedbackService, categoryRepository);
  }

  @Bean
  public GetStoreDetailsUseCase getStoreDetailsUseCase(
      GetThriftStoreUseCase getThriftStoreUseCase,
      StoreFeedbackService storeFeedbackService,
      StoreFeedbackRepository storeFeedbackRepository,
      GetStoreContentsUseCase getStoreContentsUseCase) {
    return new GetStoreDetailsUseCase(
        getThriftStoreUseCase,
        storeFeedbackService,
        storeFeedbackRepository,
        getStoreContentsUseCase);
  }

  @Bean
  public UpdateThriftStoreUseCase updateThriftStoreUseCase(
      ThriftStoreRepository thriftStoreRepository, StoreOwnershipService storeOwnershipService) {
    return new UpdateThriftStoreUseCase(thriftStoreRepository, storeOwnershipService);
  }

  @Bean
  public DeleteThriftStoreUseCase deleteThriftStoreUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      StoreDeletionService storeDeletionService) {
    return new DeleteThriftStoreUseCase(
        thriftStoreRepository, storeOwnershipService, storeDeletionService);
  }

  @Bean
  public RequestStorePhotoUploadsUseCase requestStorePhotoUploadsUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort) {
    return new RequestStorePhotoUploadsUseCase(
        thriftStoreRepository, storeOwnershipService, photoStoragePort);
  }

  @Bean
  public ReplaceStorePhotosUseCase replaceStorePhotosUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort,
      com.edufelip.meer.service.moderation.ModerationPolicyService moderationPolicyService) {
    return new ReplaceStorePhotosUseCase(
        thriftStoreRepository, storeOwnershipService, photoStoragePort, moderationPolicyService);
  }

  @Bean
  public StoreDeletionService storeDeletionService(
      ThriftStoreRepository thriftStoreRepository,
      AuthUserRepository authUserRepository,
      StoreFeedbackRepository storeFeedbackRepository,
      GuideContentRepository guideContentRepository,
      GuideContentCommentRepository guideContentCommentRepository,
      GuideContentLikeRepository guideContentLikeRepository,
      AssetDeletionQueuePort assetDeletionQueuePort) {
    return new StoreDeletionService(
        thriftStoreRepository,
        authUserRepository,
        storeFeedbackRepository,
        guideContentRepository,
        guideContentCommentRepository,
        guideContentLikeRepository,
        assetDeletionQueuePort);
  }

  @Bean
  public GetCategoriesUseCase getCategoriesUseCase(CategoryRepository repo) {
    return new GetCategoriesUseCase(repo);
  }

  @Bean
  public CreateCategoryUseCase createCategoryUseCase(CategoryRepository repo) {
    return new CreateCategoryUseCase(repo);
  }

  @Bean
  public UpdateCategoryUseCase updateCategoryUseCase(CategoryRepository repo) {
    return new UpdateCategoryUseCase(repo);
  }

  @Bean
  public DeleteCategoryUseCase deleteCategoryUseCase(CategoryRepository repo) {
    return new DeleteCategoryUseCase(repo);
  }

  @Bean
  public GetGuideContentUseCase getGuideContentUseCase(GuideContentRepository repo) {
    return new GetGuideContentUseCase(repo);
  }

  @Bean
  public GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase(
      GuideContentRepository repo) {
    return new GetGuideContentsByThriftStoreUseCase(repo);
  }

  @Bean
  public CreateGuideContentUseCase createGuideContentUseCase(
      GuideContentRepository repo, ThriftStoreRepository storeRepo) {
    return new CreateGuideContentUseCase(repo, storeRepo);
  }

  @Bean
  public CreateOwnedGuideContentUseCase createOwnedGuideContentUseCase(
      ThriftStoreRepository thriftStoreRepository,
      StoreOwnershipService storeOwnershipService,
      CreateGuideContentUseCase createGuideContentUseCase) {
    return new CreateOwnedGuideContentUseCase(
        thriftStoreRepository, storeOwnershipService, createGuideContentUseCase);
  }

  @Bean
  public RequestGuideContentImageUploadUseCase requestGuideContentImageUploadUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort) {
    return new RequestGuideContentImageUploadUseCase(
        guideContentRepository, storeOwnershipService, photoStoragePort);
  }

  @Bean
  public UpdateGuideContentUseCase updateGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService,
      PhotoStoragePort photoStoragePort,
      com.edufelip.meer.service.moderation.ModerationPolicyService moderationPolicyService) {
    return new UpdateGuideContentUseCase(
        guideContentRepository, storeOwnershipService, photoStoragePort, moderationPolicyService);
  }

  @Bean
  public DeleteGuideContentUseCase deleteGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      StoreOwnershipService storeOwnershipService,
      GuideContentModerationService guideContentModerationService) {
    return new DeleteGuideContentUseCase(
        guideContentRepository, storeOwnershipService, guideContentModerationService);
  }

  @Bean
  public LikeGuideContentUseCase likeGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      GuideContentLikeRepository guideContentLikeRepository,
      RateLimitPort rateLimitPort) {
    return new LikeGuideContentUseCase(
        guideContentRepository, guideContentLikeRepository, rateLimitPort);
  }

  @Bean
  public UnlikeGuideContentUseCase unlikeGuideContentUseCase(
      GuideContentRepository guideContentRepository,
      GuideContentLikeRepository guideContentLikeRepository,
      RateLimitPort rateLimitPort) {
    return new UnlikeGuideContentUseCase(
        guideContentRepository, guideContentLikeRepository, rateLimitPort);
  }

  @Bean
  public CreateGuideContentCommentUseCase createGuideContentCommentUseCase(
      GuideContentCommentRepository repo,
      GuideContentRepository guideContentRepository,
      Clock clock) {
    return new CreateGuideContentCommentUseCase(repo, guideContentRepository, clock);
  }

  @Bean
  public UpdateGuideContentCommentUseCase updateGuideContentCommentUseCase(
      GuideContentCommentRepository repo, Clock clock) {
    return new UpdateGuideContentCommentUseCase(repo, clock);
  }

  @Bean
  public DeleteUserUseCase deleteUserUseCase(
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
    return new DeleteUserUseCase(
        authUserRepository,
        thriftStoreRepository,
        storeFeedbackRepository,
        storeDeletionService,
        assetDeletionQueuePort,
        guideContentCommentRepository,
        guideContentLikeRepository,
        guideContentRepository,
        pushTokenRepository,
        passwordResetTokenRepository,
        jdbcTemplate);
  }

  @Bean
  public LoginUseCase loginUseCase(
      AuthUserRepository repo, PasswordEncoder encoder, TokenProvider tokenProvider) {
    return new LoginUseCase(repo, encoder, tokenProvider);
  }

  @Bean
  public DashboardLoginUseCase dashboardLoginUseCase(
      AuthUserRepository repo, PasswordEncoder encoder, TokenProvider tokenProvider) {
    return new DashboardLoginUseCase(repo, encoder, tokenProvider);
  }

  @Bean
  public SignupUseCase signupUseCase(
      AuthUserRepository repo, PasswordEncoder encoder, TokenProvider tokenProvider) {
    return new SignupUseCase(repo, encoder, tokenProvider);
  }

  @Bean
  public GoogleLoginUseCase googleLoginUseCase(
      AuthUserRepository repo,
      TokenProvider tokenProvider,
      PasswordEncoder encoder,
      GoogleClientProperties googleProps) {
    return new GoogleLoginUseCase(repo, tokenProvider, encoder, googleProps);
  }

  @Bean
  public AppleLoginUseCase appleLoginUseCase(
      AuthUserRepository repo, TokenProvider tokenProvider, PasswordEncoder encoder) {
    return new AppleLoginUseCase(repo, tokenProvider, encoder);
  }

  @Bean
  public RefreshTokenUseCase refreshTokenUseCase(
      TokenProvider tokenProvider, AuthUserRepository repo) {
    return new RefreshTokenUseCase(tokenProvider, repo);
  }

  @Bean
  public PasswordResetTokenService passwordResetTokenService(
      PasswordResetTokenRepository repository, Clock clock) {
    return new PasswordResetTokenService(repository, clock);
  }

  @Bean
  public ForgotPasswordUseCase forgotPasswordUseCase(
      AuthUserRepository repo,
      PasswordResetTokenService passwordResetTokenService,
      PasswordResetNotifier passwordResetNotifier,
      PasswordResetProperties passwordResetProperties) {
    return new ForgotPasswordUseCase(
        repo, passwordResetTokenService, passwordResetNotifier, passwordResetProperties);
  }

  @Bean
  public ResetPasswordUseCase resetPasswordUseCase(
      PasswordResetTokenRepository passwordResetTokenRepository,
      AuthUserRepository authUserRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    return new ResetPasswordUseCase(
        passwordResetTokenRepository, authUserRepository, passwordEncoder, clock);
  }

  @Bean
  public GetProfileUseCase getProfileUseCase(TokenProvider tokenProvider, AuthUserRepository repo) {
    return new GetProfileUseCase(tokenProvider, repo);
  }

  @Bean
  public UpdateProfileUseCase updateProfileUseCase(
      TokenProvider tokenProvider, AuthUserRepository repo) {
    return new UpdateProfileUseCase(tokenProvider, repo);
  }

  @Bean
  public AcceptTermsUseCase acceptTermsUseCase(AuthUserRepository repo, Clock clock) {
    return new AcceptTermsUseCase(repo, clock);
  }

  @Bean
  public UpsertPushTokenUseCase upsertPushTokenUseCase(
      PushTokenRepository pushTokenRepository, Clock clock) {
    return new UpsertPushTokenUseCase(pushTokenRepository, clock);
  }

  @Bean
  public DeletePushTokenUseCase deletePushTokenUseCase(PushTokenRepository pushTokenRepository) {
    return new DeletePushTokenUseCase(pushTokenRepository);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public TokenProvider tokenProvider(JwtProperties props, Clock clock) {
    return new JwtTokenProvider(props, clock);
  }

  @Bean
  public FilterRegistrationBean<RequestGuardsFilter> requestGuardsFilter(
      SecurityProperties securityProps,
      TokenProvider tokenProvider,
      AuthUserRepository authUserRepository) {
    FilterRegistrationBean<RequestGuardsFilter> registration =
        new FilterRegistrationBean<>(
            new RequestGuardsFilter(securityProps, tokenProvider, authUserRepository));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  public FilterRegistrationBean<RequestResponseLoggingFilter> requestResponseLoggingFilter() {
    FilterRegistrationBean<RequestResponseLoggingFilter> registration =
        new FilterRegistrationBean<>(new RequestResponseLoggingFilter());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // run right after guards
    return registration;
  }

  @Bean
  public FilterRegistrationBean<DashboardAdminGuardFilter> dashboardAdminGuardFilter(
      TokenProvider tokenProvider, AuthUserRepository authUserRepository) {
    FilterRegistrationBean<DashboardAdminGuardFilter> registration =
        new FilterRegistrationBean<>(
            new DashboardAdminGuardFilter(tokenProvider, authUserRepository));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2); // after logging
    return registration;
  }
}
