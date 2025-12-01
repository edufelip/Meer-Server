package com.edufelip.meer.domain.auth;

public class AuthenticatedUser {
    private final java.util.UUID id;
    private final String name;
    private final String email;

    public AuthenticatedUser(java.util.UUID id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public java.util.UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
