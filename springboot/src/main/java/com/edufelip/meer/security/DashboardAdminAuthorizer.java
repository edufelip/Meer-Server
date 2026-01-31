package com.edufelip.meer.security;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DashboardAdminAuthorizer {

  public AuthUser requireAdmin(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
    }
    AuthUser user = resolveAdminFromRequest();
    Role effectiveRole = user.getRole() != null ? user.getRole() : Role.USER;
    if (effectiveRole != Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
    }
    return user;
  }

  private AuthUser resolveAdminFromRequest() {
    try {
      Object cached =
          RequestContextHolder.currentRequestAttributes()
              .getAttribute("adminUser", RequestAttributes.SCOPE_REQUEST);
      if (cached instanceof AuthUser cachedUser) {
        return cachedUser;
      }
    } catch (IllegalStateException ignored) {
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing admin context");
  }
}
