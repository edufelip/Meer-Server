package com.edufelip.meer.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    val requireAppHeader: Boolean = true,
    val requireAppCheck: Boolean = true,
    val disableAuth: Boolean = false,
    val appPackage: String = "com.edufelip.meer"
)
