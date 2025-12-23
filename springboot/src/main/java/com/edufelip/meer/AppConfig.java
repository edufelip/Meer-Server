package com.edufelip.meer;

import com.edufelip.meer.domain.CreateGuideContentUseCase;
import com.edufelip.meer.domain.CreateThriftStoreUseCase;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.GetGuideContentsByThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.auth.AppleLoginUseCase;
import com.edufelip.meer.domain.auth.DashboardLoginUseCase;
import com.edufelip.meer.domain.auth.ForgotPasswordUseCase;
import com.edufelip.meer.domain.auth.GetProfileUseCase;
import com.edufelip.meer.domain.auth.GoogleLoginUseCase;
import com.edufelip.meer.domain.auth.LoginUseCase;
import com.edufelip.meer.domain.auth.RefreshTokenUseCase;
import com.edufelip.meer.domain.auth.SignupUseCase;
import com.edufelip.meer.domain.auth.UpdateProfileUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.logging.RequestResponseLoggingFilter;
import com.edufelip.meer.security.DashboardAdminGuardFilter;
import com.edufelip.meer.security.GoogleClientProperties;
import com.edufelip.meer.security.JwtProperties;
import com.edufelip.meer.security.RequestGuardsFilter;
import com.edufelip.meer.security.SecurityProperties;
import com.edufelip.meer.security.token.JwtTokenProvider;
import com.edufelip.meer.security.token.TokenProvider;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableCaching
@EnableConfigurationProperties({
  SecurityProperties.class,
  JwtProperties.class,
  GoogleClientProperties.class
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
  public CreateThriftStoreUseCase createThriftStoreUseCase(ThriftStoreRepository repo) {
    return new CreateThriftStoreUseCase(repo);
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
  public ForgotPasswordUseCase forgotPasswordUseCase(AuthUserRepository repo) {
    return new ForgotPasswordUseCase(repo);
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
      SecurityProperties securityProps) {
    FilterRegistrationBean<RequestGuardsFilter> registration =
        new FilterRegistrationBean<>(new RequestGuardsFilter(securityProps));
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
