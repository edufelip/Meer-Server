package com.edufelip.meer.security.guards;

import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.SecurityProperties;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;

public class FirebaseAuthGuard {
  public static final String AUTH_HEADER = "Authorization";
  private final SecurityProperties props;
  private final TokenProvider tokenProvider;
  private final AuthUserRepository authUserRepository;

  public FirebaseAuthGuard(
      SecurityProperties props,
      TokenProvider tokenProvider,
      AuthUserRepository authUserRepository) {
    this.props = props;
    this.tokenProvider = tokenProvider;
    this.authUserRepository = authUserRepository;
  }

  public void validate(HttpServletRequest request) {
    if (props.isDisableAuth()) return;
    String header = request.getHeader(AUTH_HEADER);
    if (header == null || !header.startsWith("Bearer ")) {
      throw new GuardException("Missing or invalid " + AUTH_HEADER + " header");
    }
    String token = header.substring("Bearer ".length()).trim();
    if (token.isBlank()) {
      throw new GuardException("Missing or invalid " + AUTH_HEADER + " header");
    }
    try {
      TokenPayload payload = tokenProvider.parseAccessToken(token);
      if (authUserRepository.findById(payload.getUserId()).isEmpty()) {
        throw new GuardException("User not found");
      }
    } catch (GuardException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new GuardException("Invalid token");
    }
  }
}
