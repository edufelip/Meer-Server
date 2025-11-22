package com.edufelip.meer

import com.edufelip.meer.domain.*
import com.edufelip.meer.security.RequestGuardsFilter
import com.edufelip.meer.security.SecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered

@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
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
    fun requestGuardsFilter(securityProperties: SecurityProperties): FilterRegistrationBean<RequestGuardsFilter> {
        val registration = FilterRegistrationBean(RequestGuardsFilter(securityProperties))
        registration.order = Ordered.HIGHEST_PRECEDENCE
        return registration
    }
}
