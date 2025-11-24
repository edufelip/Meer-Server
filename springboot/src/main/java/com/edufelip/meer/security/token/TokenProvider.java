package com.edufelip.meer.security.token;

import com.edufelip.meer.core.auth.AuthUser;

public interface TokenProvider {
    String generateAccessToken(AuthUser user);
    String generateRefreshToken(AuthUser user);
    TokenPayload parseAccessToken(String token);
    TokenPayload parseRefreshToken(String token);
}
