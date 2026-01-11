package com.edufelip.meer.core.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "asset_deletion_job")
public class AssetDeletionJob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 2048)
  private String url;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private AssetDeletionStatus status = AssetDeletionStatus.PENDING;

  @Column(nullable = false)
  private int attempts = 0;

  @Column(nullable = false)
  private Instant nextAttemptAt;

  @Column(length = 1024)
  private String lastError;

  @Column(length = 64)
  private String sourceType;

  @Column(length = 64)
  private String sourceId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public AssetDeletionJob() {}

  public AssetDeletionJob(String url, Instant nextAttemptAt, String sourceType, String sourceId) {
    this.url = url;
    this.nextAttemptAt = nextAttemptAt;
    this.sourceType = sourceType;
    this.sourceId = sourceId;
  }

  public Long getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public AssetDeletionStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public String getLastError() {
    return lastError;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getSourceId() {
    return sourceId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setStatus(AssetDeletionStatus status) {
    this.status = status;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public void setNextAttemptAt(Instant nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
