package com.edufelip.meer.security.token;

public class TokenPayload {
    private final Integer userId;
    private final String email;
    private final String name;

    public TokenPayload(Integer userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    public Integer getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
}
