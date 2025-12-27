package com.edufelip.meer.core.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID token;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "auth_user_id", columnDefinition = "uuid", nullable = false)
  private AuthUser user;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  public PasswordResetToken() {}

  public PasswordResetToken(UUID token, AuthUser user, Instant expiresAt) {
    this.token = token;
    this.user = user;
    this.expiresAt = expiresAt;
  }

  public UUID getToken() {
    return token;
  }

  public AuthUser getUser() {
    return user;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void setToken(UUID token) {
    this.token = token;
  }

  public void setUser(AuthUser user) {
    this.user = user;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }
}
