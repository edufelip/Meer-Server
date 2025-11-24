package com.edufelip.meer.web

import com.edufelip.meer.domain.auth.InvalidCredentialsException
import com.edufelip.meer.domain.auth.LoginUseCase
import com.edufelip.meer.domain.auth.SignupUseCase
import com.edufelip.meer.domain.auth.EmailAlreadyRegisteredException
import com.edufelip.meer.domain.auth.GoogleLoginUseCase
import com.edufelip.meer.domain.auth.InvalidGoogleTokenException
import com.edufelip.meer.domain.auth.RefreshTokenUseCase
import com.edufelip.meer.domain.auth.AppleLoginUseCase
import com.edufelip.meer.domain.auth.InvalidAppleTokenException
import com.edufelip.meer.domain.auth.ForgotPasswordUseCase
import com.edufelip.meer.domain.AuthUserRepository
import com.edufelip.meer.dto.AuthResponse
import com.edufelip.meer.dto.LoginRequest
import com.edufelip.meer.dto.SignupRequest
import com.edufelip.meer.dto.GoogleLoginRequest
import com.edufelip.meer.dto.RefreshRequest
import com.edufelip.meer.dto.AppleLoginRequest
import com.edufelip.meer.dto.ForgotPasswordRequest
import com.edufelip.meer.mapper.toDto
import com.edufelip.meer.security.token.InvalidTokenException
import com.edufelip.meer.security.token.TokenProvider
import com.edufelip.meer.security.token.InvalidRefreshTokenException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val signupUseCase: SignupUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val appleLoginUseCase: AppleLoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val tokenProvider: TokenProvider,
    private val authUserRepository: AuthUserRepository
) {

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest): ResponseEntity<AuthResponse> {
        val result = loginUseCase.execute(body.email, body.password)
        return ResponseEntity.ok(
            AuthResponse(
                token = result.token,
                refreshToken = result.refreshToken,
                user = result.user.toDto()
            )
        )
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody body: RefreshRequest): ResponseEntity<AuthResponse> {
        val result = refreshTokenUseCase.execute(body.refreshToken)
        return ResponseEntity.ok(
            AuthResponse(
                token = result.token,
                refreshToken = result.refreshToken,
                user = result.user.toDto()
            )
        )
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody body: ForgotPasswordRequest): ResponseEntity<Map<String, String>> {
        forgotPasswordUseCase.execute(body.email)
        return ResponseEntity.ok(mapOf("message" to "Reset email sent"))
    }

    @PostMapping("/google")
    fun loginWithGoogle(
        @RequestBody body: GoogleLoginRequest,
        @RequestHeader(name = "Authorization", required = false) authHeader: String?
    ): ResponseEntity<AuthResponse> {
        val idToken = body.idToken ?: extractBearerOptional(authHeader) ?: throw InvalidGoogleTokenException()
        val result = googleLoginUseCase.execute(idToken, body.client)
        return ResponseEntity.ok(
            AuthResponse(
                token = result.token,
                refreshToken = result.refreshToken,
                user = result.user.toDto()
            )
        )
    }

    @PostMapping("/apple")
    fun loginWithApple(@RequestBody body: AppleLoginRequest): ResponseEntity<AuthResponse> {
        if (body.provider.lowercase() != "apple") throw InvalidAppleTokenException()
        val result = appleLoginUseCase.execute(body.idToken, body.authorizationCode, body.client)
        return ResponseEntity.ok(
            AuthResponse(
                token = result.token,
                refreshToken = result.refreshToken,
                user = result.user.toDto()
            )
        )
    }

    @PostMapping("/signup")
    fun signup(@RequestBody body: SignupRequest): ResponseEntity<AuthResponse> {
        val result = signupUseCase.execute(body.name, body.email, body.password)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AuthResponse(
                token = result.token,
                refreshToken = result.refreshToken,
                user = result.user.toDto()
            )
        )
    }

    @GetMapping("/me")
    fun me(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Map<String, Any?>> {
        val token = extractBearer(authHeader)
        val payload = tokenProvider.parseAccessToken(token)
        val user = authUserRepository.findById(payload.userId).orElseThrow { InvalidTokenException() }
        val userDto = user.let { com.edufelip.meer.dto.UserDto(id = it.id.toString(), name = it.displayName, email = it.email) }
        return ResponseEntity.ok(mapOf("user" to userDto))
    }

    private fun extractBearer(header: String?): String {
        if (header == null || !header.startsWith("Bearer ")) throw InvalidTokenException()
        return header.removePrefix("Bearer ").trim()
    }

    private fun extractBearerOptional(header: String?): String? {
        if (header == null || !header.startsWith("Bearer ")) return null
        return header.removePrefix("Bearer ").trim()
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to ex.message!!))
    }

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun handleEmailTaken(ex: EmailAlreadyRegisteredException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to ex.message!!))
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to ex.message!!))
    }

    @ExceptionHandler(InvalidGoogleTokenException::class)
    fun handleInvalidGoogleToken(ex: InvalidGoogleTokenException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to ex.message!!))
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefresh(ex: InvalidRefreshTokenException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to ex.message!!))
    }

    @ExceptionHandler(InvalidAppleTokenException::class)
    fun handleInvalidApple(ex: InvalidAppleTokenException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to ex.message!!))
    }
}
