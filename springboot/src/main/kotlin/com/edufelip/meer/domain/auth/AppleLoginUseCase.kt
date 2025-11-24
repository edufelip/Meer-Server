package com.edufelip.meer.domain.auth

import com.edufelip.meer.core.auth.AuthUser
import com.edufelip.meer.domain.AuthUserRepository
import com.edufelip.meer.security.token.TokenProvider
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class AppleLoginUseCase(
    private val authUserRepository: AuthUserRepository,
    private val tokenProvider: TokenProvider,
    private val passwordEncoder: PasswordEncoder
) {
    private val mapper = jacksonObjectMapper()

    fun execute(idToken: String, authorizationCode: String?, client: String): AuthResult {
        if (client.lowercase() != "ios") throw InvalidAppleTokenException()
        val payload = decodePayload(idToken)

        val email = payload["email"] as? String ?: throw InvalidAppleTokenException()
        val name = (payload["name"] as? String) ?: email

        val randomPassword = UUID.randomUUID().toString()
        val hashed = passwordEncoder.encode(randomPassword).toString()
        val user = authUserRepository.findByEmail(email) ?: authUserRepository.save(
            AuthUser(
                email = email,
                displayName = name,
                passwordHash = hashed
            )
        )

        val accessToken = tokenProvider.generateAccessToken(user)
        val refreshToken = tokenProvider.generateRefreshToken(user)

        return AuthResult(
            token = accessToken,
            refreshToken = refreshToken,
            user = AuthenticatedUser(id = user.id, name = user.displayName, email = user.email)
        )
    }

    private fun decodePayload(idToken: String): Map<String, Any?> {
        val parts = idToken.split(".")
        if (parts.size < 2) throw InvalidAppleTokenException()
        val payloadPart = parts[1]
        val decoded = Base64.getUrlDecoder().decode(payloadPart)
        return mapper.readValue(decoded)
    }
}

class InvalidAppleTokenException : RuntimeException("Invalid or expired token")
