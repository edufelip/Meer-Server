package com.edufelip.meer.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class GoogleLoginRequest(
    val client: String,
    val idToken: String? = null
)

data class RefreshRequest(
    val refreshToken: String
)

data class AppleLoginRequest(
    val provider: String,
    val idToken: String,
    val authorizationCode: String? = null,
    val client: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class UserDto(
    val id: String,
    val name: String?,
    val email: String?
)

data class AuthResponse(
    val token: String,
    val refreshToken: String?,
    val user: UserDto
)
