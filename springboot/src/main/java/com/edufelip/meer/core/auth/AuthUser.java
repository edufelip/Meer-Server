package com.edufelip.meer.core.auth;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.util.Uuid7;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
public class AuthUser {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String displayName;

    private String photoUrl;

    @Column(length = 1000)
    private String bio;

    @Column(nullable = false)
    private boolean notifyNewStores = true;

    @Column(nullable = false)
    private boolean notifyPromos = true;

    @OneToOne
    @JoinColumn(name = "owned_thrift_store_id", columnDefinition = "uuid")
    private ThriftStore ownedThriftStore;

    @ManyToMany
    @JoinTable(
            name = "auth_user_favorites",
            joinColumns = @JoinColumn(name = "auth_user_id", columnDefinition = "uuid"),
            inverseJoinColumns = @JoinColumn(name = "thrift_store_id", columnDefinition = "uuid")
    )
    private Set<ThriftStore> favorites = new HashSet<>();

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public AuthUser() {}

    public AuthUser(UUID id, String email, String displayName, String photoUrl, String passwordHash) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.passwordHash = passwordHash;
    }

    public AuthUser(String email, String displayName, String photoUrl, String passwordHash) {
        this(null, email, displayName, photoUrl, passwordHash);
    }

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = Uuid7.next();
        }
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPhotoUrl() { return photoUrl; }
    public String getBio() { return bio; }
    public boolean isNotifyNewStores() { return notifyNewStores; }
    public boolean isNotifyPromos() { return notifyPromos; }
    public ThriftStore getOwnedThriftStore() { return ownedThriftStore; }
    public String getPasswordHash() { return passwordHash; }
    public Set<ThriftStore> getFavorites() { return favorites; }

    public void setId(UUID id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setNotifyNewStores(boolean notifyNewStores) { this.notifyNewStores = notifyNewStores; }
    public void setNotifyPromos(boolean notifyPromos) { this.notifyPromos = notifyPromos; }
    public void setOwnedThriftStore(ThriftStore ownedThriftStore) { this.ownedThriftStore = ownedThriftStore; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFavorites(Set<ThriftStore> favorites) { this.favorites = favorites; }
}
