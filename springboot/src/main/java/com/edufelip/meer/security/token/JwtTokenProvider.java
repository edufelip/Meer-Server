package com.edufelip.meer.security.token;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.security.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class JwtTokenProvider implements TokenProvider {
  private final JwtProperties props;
  private final Key key;
  private final Clock clock;
  private final io.jsonwebtoken.Clock jjwtClock;

  public JwtTokenProvider(JwtProperties props, Clock clock) {
    this.props = props;
    this.clock = clock;
    this.key = buildKey(props.getSecret());
    this.jjwtClock = () -> Date.from(Instant.now(clock));
  }

  @Override
  public String generateAccessToken(AuthUser user) {
    Instant now = Instant.now(clock);
    Instant exp = now.plusSeconds(props.getAccessTtlMinutes() * 60);
    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("name", user.getDisplayName())
        .claim("role", user.getRole() != null ? user.getRole().name() : Role.USER.name())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  @Override
  public String generateRefreshToken(AuthUser user) {
    Instant now = Instant.now(clock);
    Instant exp = now.plusSeconds(props.getRefreshTtlDays() * 24 * 60 * 60);
    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("type", "refresh")
        .claim("email", user.getEmail())
        .claim("name", user.getDisplayName())
        .claim("role", user.getRole() != null ? user.getRole().name() : Role.USER.name())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  @Override
  public TokenPayload parseAccessToken(String token) {
    try {
      var claims =
          Jwts.parserBuilder()
              .setSigningKey(key)
              .setClock(jjwtClock)
              .build()
              .parseClaimsJws(token)
              .getBody();
      UUID userId = UUID.fromString(claims.getSubject());
      Role role = Role.USER;
      Object roleClaim = claims.get("role");
      if (roleClaim instanceof String s) {
        try {
          role = Role.valueOf(s);
        } catch (IllegalArgumentException ignored) {
        }
      }
      return new TokenPayload(
          userId, (String) claims.get("email"), (String) claims.get("name"), role);
    } catch (JwtException | IllegalArgumentException ex) {
      throw new InvalidTokenException();
    }
  }

  @Override
  public TokenPayload parseRefreshToken(String token) {
    try {
      var claims =
          Jwts.parserBuilder()
              .setSigningKey(key)
              .setClock(jjwtClock)
              .build()
              .parseClaimsJws(token)
              .getBody();
      if (!"refresh".equals(claims.get("type"))) throw new InvalidRefreshTokenException();
      UUID userId = UUID.fromString(claims.getSubject());
      Role role = Role.USER;
      Object roleClaim = claims.get("role");
      if (roleClaim instanceof String s) {
        try {
          role = Role.valueOf(s);
        } catch (IllegalArgumentException ignored) {
        }
      }
      return new TokenPayload(
          userId, (String) claims.get("email"), (String) claims.get("name"), role);
    } catch (JwtException | IllegalArgumentException ex) {
      throw new InvalidRefreshTokenException();
    }
  }

  private Key buildKey(String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length * 8 < 256) {
      throw new IllegalArgumentException(
          "SECURITY_JWT_SECRET must be at least 32 bytes (256 bits); current length is "
              + bytes.length
              + " bytes");
    }
    return Keys.hmacShaKeyFor(bytes);
  }
}
