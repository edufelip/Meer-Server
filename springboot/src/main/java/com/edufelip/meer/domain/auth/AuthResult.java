package com.edufelip.meer.domain.auth;

public class AuthResult {
    private final String token;
    private final String refreshToken;
    private final AuthenticatedUser user;

    public AuthResult(String token, String refreshToken, AuthenticatedUser user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public AuthenticatedUser getUser() { return user; }
}
