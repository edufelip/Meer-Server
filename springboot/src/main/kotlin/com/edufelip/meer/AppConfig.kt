package com.edufelip.meer

import com.edufelip.meer.domain.*
import com.edufelip.meer.security.GoogleClientProperties
import com.edufelip.meer.security.JwtProperties
import com.edufelip.meer.security.RequestGuardsFilter
import com.edufelip.meer.security.SecurityProperties
import com.edufelip.meer.security.token.JwtTokenProvider
import com.edufelip.meer.security.token.TokenProvider
import com.edufelip.meer.domain.auth.SignupUseCase
import com.edufelip.meer.domain.auth.GoogleLoginUseCase
import com.edufelip.meer.domain.auth.RefreshTokenUseCase
import com.edufelip.meer.domain.auth.AppleLoginUseCase
import com.edufelip.meer.domain.auth.ForgotPasswordUseCase
import com.edufelip.meer.domain.auth.LoginUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@EnableConfigurationProperties(value = [SecurityProperties::class, JwtProperties::class, GoogleClientProperties::class])
class AppConfig {

    @Bean
    fun getThriftStoreUseCase(thriftStoreRepository: ThriftStoreRepository): GetThriftStoreUseCase {
        return GetThriftStoreUseCase(thriftStoreRepository)
    }

    @Bean
    fun getThriftStoresUseCase(thriftStoreRepository: ThriftStoreRepository): GetThriftStoresUseCase {
        return GetThriftStoresUseCase(thriftStoreRepository)
    }

    @Bean
    fun createThriftStoreUseCase(thriftStoreRepository: ThriftStoreRepository): CreateThriftStoreUseCase {
        return CreateThriftStoreUseCase(thriftStoreRepository)
    }

    @Bean
    fun getGuideContentUseCase(guideContentRepository: GuideContentRepository): GetGuideContentUseCase {
        return GetGuideContentUseCase(guideContentRepository)
    }

    @Bean
    fun getGuideContentsByThriftStoreUseCase(guideContentRepository: GuideContentRepository): GetGuideContentsByThriftStoreUseCase {
        return GetGuideContentsByThriftStoreUseCase(guideContentRepository)
    }

    @Bean
    fun createGuideContentUseCase(
        guideContentRepository: GuideContentRepository,
        thriftStoreRepository: ThriftStoreRepository
    ): CreateGuideContentUseCase {
        return CreateGuideContentUseCase(guideContentRepository, thriftStoreRepository)
    }

    @Bean
    fun signupUseCase(
        authUserRepository: AuthUserRepository,
        passwordEncoder: PasswordEncoder,
        tokenProvider: TokenProvider
    ): SignupUseCase = SignupUseCase(authUserRepository, passwordEncoder, tokenProvider)

    @Bean
    fun googleLoginUseCase(
        authUserRepository: AuthUserRepository,
        tokenProvider: TokenProvider,
        passwordEncoder: PasswordEncoder,
        googleClientProperties: GoogleClientProperties
    ): GoogleLoginUseCase = GoogleLoginUseCase(authUserRepository, tokenProvider, passwordEncoder, googleClientProperties)

    @Bean
    fun refreshTokenUseCase(
        tokenProvider: TokenProvider,
        authUserRepository: AuthUserRepository
    ): RefreshTokenUseCase = RefreshTokenUseCase(tokenProvider, authUserRepository)

    @Bean
    fun appleLoginUseCase(
        authUserRepository: AuthUserRepository,
        tokenProvider: TokenProvider,
        passwordEncoder: PasswordEncoder
    ): AppleLoginUseCase = AppleLoginUseCase(authUserRepository, tokenProvider, passwordEncoder)

    @Bean
    fun forgotPasswordUseCase(
        authUserRepository: AuthUserRepository
    ): ForgotPasswordUseCase = ForgotPasswordUseCase(authUserRepository)

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun tokenProvider(jwtProperties: JwtProperties): TokenProvider = JwtTokenProvider(jwtProperties)

    @Bean
    fun loginUseCase(
        authUserRepository: AuthUserRepository,
        passwordEncoder: PasswordEncoder,
        tokenProvider: TokenProvider
    ): LoginUseCase = LoginUseCase(authUserRepository, passwordEncoder, tokenProvider)

    @Bean
    fun requestGuardsFilter(securityProperties: SecurityProperties): FilterRegistrationBean<RequestGuardsFilter> {
        val registration = FilterRegistrationBean(RequestGuardsFilter(securityProperties))
        registration.order = Ordered.HIGHEST_PRECEDENCE
        return registration
    }
}
