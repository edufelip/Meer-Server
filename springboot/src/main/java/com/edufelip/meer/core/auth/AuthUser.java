package com.edufelip.meer.core.auth;

import jakarta.persistence.*;

@Entity
public class AuthUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String email;

    private String displayName;

    private String photoUrl;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public AuthUser() {}

    public AuthUser(Integer id, String email, String displayName, String photoUrl, String passwordHash) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.passwordHash = passwordHash;
    }

    public AuthUser(String email, String displayName, String photoUrl, String passwordHash) {
        this(null, email, displayName, photoUrl, passwordHash);
    }

    public Integer getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPhotoUrl() { return photoUrl; }
    public String getPasswordHash() { return passwordHash; }

    public void setId(Integer id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
