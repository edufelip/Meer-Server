package com.edufelip.meer.security;

import com.edufelip.meer.security.guards.AppHeaderGuard;
import com.edufelip.meer.security.guards.FirebaseAppCheckGuard;
import com.edufelip.meer.security.guards.FirebaseAuthGuard;
import com.edufelip.meer.security.guards.GuardException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestGuardsFilter extends OncePerRequestFilter {

    private final AppHeaderGuard appHeaderGuard;
    private final FirebaseAppCheckGuard appCheckGuard;
    private final FirebaseAuthGuard authGuard;

    public RequestGuardsFilter(SecurityProperties securityProps) {
        this.appHeaderGuard = new AppHeaderGuard(securityProps);
        this.appCheckGuard = new FirebaseAppCheckGuard(securityProps);
        this.authGuard = new FirebaseAuthGuard(securityProps);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        if (isPublicAuthPath(request)) {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        try {
            appHeaderGuard.validate(request);
            appCheckGuard.validate(request);
            authGuard.validate(request);
            filterChain.doFilter(request, response);
        } catch (GuardException ex) {
            sendUnauthorized(response, ex.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isPublicAuthPath(HttpServletRequest request) {
        String path = request.getServletPath().toLowerCase();
        return switch (path) {
            case "/auth/login", "/auth/signup", "/auth/google", "/auth/apple", "/auth/refresh", "/auth/forgot-password" -> true;
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
