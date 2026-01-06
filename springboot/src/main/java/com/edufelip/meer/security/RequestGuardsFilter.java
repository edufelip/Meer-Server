package com.edufelip.meer.security;

import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.guards.AppHeaderGuard;
import com.edufelip.meer.security.guards.FirebaseAuthGuard;
import com.edufelip.meer.security.guards.GuardException;
import com.edufelip.meer.security.token.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestGuardsFilter extends OncePerRequestFilter {

  private final AppHeaderGuard appHeaderGuard;
  private final FirebaseAuthGuard authGuard;

  public RequestGuardsFilter(
      SecurityProperties securityProps,
      TokenProvider tokenProvider,
      AuthUserRepository authUserRepository) {
    this.appHeaderGuard = new AppHeaderGuard(securityProps);
    this.authGuard = new FirebaseAuthGuard(securityProps, tokenProvider, authUserRepository);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws jakarta.servlet.ServletException, java.io.IOException {
    // Preflight requests should bypass auth/header guards
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      if (isPublicPath(request)) {
        if (shouldRequireAppHeader(request)) {
          appHeaderGuard.validate(request);
        }
        String authHeader = request.getHeader(FirebaseAuthGuard.AUTH_HEADER);
        if (authHeader != null) {
          authGuard.validate(request);
        }
        filterChain.doFilter(request, response);
        return;
      }
      appHeaderGuard.validate(request);
      authGuard.validate(request);
      filterChain.doFilter(request, response);
    } catch (GuardException ex) {
      sendUnauthorized(response, ex.getMessage());
    }
  }

  private boolean isPublicPath(HttpServletRequest request) {
    String path = request.getServletPath().toLowerCase();
    String method = request.getMethod().toUpperCase();

    // Public read-only endpoints
    if ("GET".equals(method)) {
      if (path.equals("/contents") || path.startsWith("/contents/")) return true;
      if (path.equals("/home")
          || path.equals("/featured")
          || path.equals("/nearby")
          || path.equals("/stores")
          || path.equals("/stores/search")
          || path.equals("/categories")) {
        return true;
      }
      if (path.startsWith("/categories/") && path.endsWith("/stores")) return true;
      if (path.startsWith("/stores/")) {
        if (path.endsWith("/contents") || path.endsWith("/ratings")) return true;
        String suffix = path.substring("/stores/".length());
        return !suffix.isBlank() && !suffix.contains("/");
      }
    }

    // Legacy explicit allowlist
    return switch (path) {
      case "/actuator/health",
              "/actuator/health/liveness",
              "/actuator/health/readiness",
              "/actuator/info" ->
          true;
      case "/auth/login",
              "/auth/signup",
              "/auth/google",
              "/auth/apple",
              "/auth/refresh",
              "/auth/forgot-password",
              "/auth/reset-password",
              "/dashboard/login" ->
          true;
      case "/support/contact" -> true;
      default -> false;
    };
  }

  private boolean shouldRequireAppHeader(HttpServletRequest request) {
    String path = request.getServletPath().toLowerCase();
    String method = request.getMethod().toUpperCase();
    if (!"GET".equals(method)) return false;
    if (path.startsWith("/actuator")) return false;
    if (path.startsWith("/dashboard")) return false;
    return true;
  }

  private void sendUnauthorized(HttpServletResponse response, String message) {
    try {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    } catch (Exception ignored) {
    }
  }
}
