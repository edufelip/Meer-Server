package com.edufelip.meer.core.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity that tracks image moderation status and results. Used for NSFW content detection and
 * manual review workflow.
 */
@Entity
@Table(
    name = "image_moderation",
    indexes = {
      @Index(name = "idx_image_moderation_status", columnList = "status"),
      @Index(name = "idx_image_moderation_image_url", columnList = "imageUrl"),
      @Index(name = "idx_image_moderation_entity", columnList = "entityType,entityId"),
      @Index(name = "idx_image_moderation_created_at", columnList = "createdAt")
    })
public class ImageModeration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 2048)
  private String imageUrl;

  @Column(nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private ModerationStatus status;

  @Column(nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Column(nullable = false, length = 255)
  private String entityId;

  @Column(nullable = true)
  private Double nsfwScore;

  @Column(nullable = true, columnDefinition = "TEXT")
  private String failureReason;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = true)
  private Instant processedAt;

  @Column(nullable = true)
  private Instant reviewedAt;

  @Column(nullable = true)
  private Instant cleanupAt;

  @Column(nullable = true, length = 255)
  private String reviewedBy;

  @Column(nullable = true, columnDefinition = "TEXT")
  private String reviewNotes;

  @Column(nullable = false)
  private int retryCount = 0;

  public ImageModeration() {
    this.createdAt = Instant.now();
    this.status = ModerationStatus.PENDING;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public ModerationStatus getStatus() {
    return status;
  }

  public void setStatus(ModerationStatus status) {
    this.status = status;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public Double getNsfwScore() {
    return nsfwScore;
  }

  public void setNsfwScore(Double nsfwScore) {
    this.nsfwScore = nsfwScore;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(Instant reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public Instant getCleanupAt() {
    return cleanupAt;
  }

  public void setCleanupAt(Instant cleanupAt) {
    this.cleanupAt = cleanupAt;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(String reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public String getReviewNotes() {
    return reviewNotes;
  }

  public void setReviewNotes(String reviewNotes) {
    this.reviewNotes = reviewNotes;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public void incrementRetryCount() {
    this.retryCount++;
  }
}
