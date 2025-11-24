package com.edufelip.meer.security.token

import com.edufelip.meer.core.auth.AuthUser
import com.edufelip.meer.security.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

interface TokenProvider {
    fun generateAccessToken(user: AuthUser): String
    fun generateRefreshToken(user: AuthUser): String?
    fun parseAccessToken(token: String): TokenPayload
    fun parseRefreshToken(token: String): TokenPayload
}

data class TokenPayload(
    val userId: Int,
    val email: String?,
    val name: String?
)

class JwtTokenProvider(private val props: JwtProperties) : TokenProvider {

    private val key = buildKey(props.secret)

    override fun generateAccessToken(user: AuthUser): String {
        val now = Instant.now()
        val exp = now.plusSeconds(props.accessTtlMinutes * 60)
        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("email", user.email)
            .claim("name", user.displayName)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(exp))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun generateRefreshToken(user: AuthUser): String {
        val now = Instant.now()
        val exp = now.plusSeconds(props.refreshTtlDays * 24 * 60 * 60)
        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("type", "refresh")
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(exp))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun parseAccessToken(token: String): TokenPayload {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        val userId = claims.subject?.toIntOrNull() ?: throw InvalidTokenException()
        return TokenPayload(
            userId = userId,
            email = claims["email"] as String?,
            name = claims["name"] as String?
        )
    }

    override fun parseRefreshToken(token: String): TokenPayload {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
        if (claims["type"] != "refresh") throw InvalidRefreshTokenException()
        val userId = claims.subject?.toIntOrNull() ?: throw InvalidRefreshTokenException()
        return TokenPayload(
            userId = userId,
            email = claims["email"] as String?,
            name = claims["name"] as String?
        )
    }
}

private fun buildKey(secret: String): java.security.Key {
    val bytes = secret.toByteArray(StandardCharsets.UTF_8)
    if (bytes.size * 8 < 256) {
        throw IllegalArgumentException("SECURITY_JWT_SECRET must be at least 32 bytes (256 bits); current length is ${bytes.size} bytes")
    }
    return Keys.hmacShaKeyFor(bytes)
}

class InvalidTokenException : RuntimeException("Invalid or expired token")
class InvalidRefreshTokenException : RuntimeException("Invalid or expired token")
