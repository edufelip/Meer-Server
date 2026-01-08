package com.edufelip.meer.security;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.stereotype.Component;

@Component
public class AuthUserResolver {

  private final TokenProvider tokenProvider;
  private final AuthUserRepository authUserRepository;

  public AuthUserResolver(TokenProvider tokenProvider, AuthUserRepository authUserRepository) {
    this.tokenProvider = tokenProvider;
    this.authUserRepository = authUserRepository;
  }

  public AuthUser requireUser(String authHeader) {
    return loadUser(requirePayload(authHeader));
  }

  public AuthUser optionalUser(String authHeader) {
    if (authHeader == null) return null;
    return requireUser(authHeader);
  }

  public TokenPayload requirePayload(String authHeader) {
    return parsePayload(requireBearer(authHeader));
  }

  public TokenPayload optionalPayload(String authHeader) {
    if (authHeader == null) return null;
    return requirePayload(authHeader);
  }

  public String requireBearer(String authHeader) {
    if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
      throw new InvalidTokenException();
    }
    return authHeader.substring("Bearer ".length()).trim();
  }

  public String optionalBearer(String authHeader) {
    if (authHeader == null) return null;
    return requireBearer(authHeader);
  }

  private TokenPayload parsePayload(String token) {
    try {
      TokenPayload payload = tokenProvider.parseAccessToken(token);
      if (payload == null) throw new InvalidTokenException();
      return payload;
    } catch (RuntimeException ex) {
      throw new InvalidTokenException();
    }
  }

  private AuthUser loadUser(TokenPayload payload) {
    return authUserRepository.findById(payload.getUserId()).orElseThrow(InvalidTokenException::new);
  }
}
