package com.edufelip.meer.security.token;

import java.util.UUID;

public class TokenPayload {
    private final UUID userId;
    private final String email;
    private final String name;

    public TokenPayload(UUID userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
}
