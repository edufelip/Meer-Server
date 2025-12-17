package com.edufelip.meer.security;

import com.edufelip.meer.security.guards.AppHeaderGuard;
import com.edufelip.meer.security.guards.FirebaseAuthGuard;
import com.edufelip.meer.security.guards.GuardException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestGuardsFilter extends OncePerRequestFilter {

    private final AppHeaderGuard appHeaderGuard;
    private final FirebaseAuthGuard authGuard;

    public RequestGuardsFilter(SecurityProperties securityProps) {
        this.appHeaderGuard = new AppHeaderGuard(securityProps);
        this.authGuard = new FirebaseAuthGuard(securityProps);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws jakarta.servlet.ServletException, java.io.IOException {
        // Preflight requests should bypass auth/header guards
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isPublicPath(request)) {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        try {
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
            if (path.equals("/contents") || path.startsWith("/contents/")) {
                return true;
            }
        }

        // Legacy explicit allowlist
        return switch (path) {
            case "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness", "/actuator/info" -> true;
            case "/auth/login", "/auth/signup", "/auth/google", "/auth/apple", "/auth/refresh", "/auth/forgot-password", "/dashboard/login" -> true;
            case "/support/contact" -> true;
            default -> false;
        };
    }

    private void sendUnauthorized(HttpServletResponse response, String message) {
        try {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
        } catch (Exception ignored) {
        }
    }
}
