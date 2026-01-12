package com.edufelip.meer.core.content;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class GuideContent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 2048)
  private String description;

  @Column(nullable = false)
  private String categoryLabel;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false, length = 512)
  private String imageUrl;

  @Column(name = "like_count", nullable = false)
  private Long likeCount = 0L;

  @Column(name = "comment_count", nullable = false)
  private Long commentCount = 0L;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "thrift_store_id", columnDefinition = "uuid")
  private ThriftStore thriftStore;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_user_id", columnDefinition = "uuid")
  private AuthUser deletedBy;

  @Column(name = "deleted_reason", length = 255)
  private String deletedReason;

  public GuideContent() {}

  public GuideContent(
      Integer id,
      String title,
      String description,
      String categoryLabel,
      String type,
      String imageUrl,
      ThriftStore thriftStore) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.categoryLabel = categoryLabel;
    this.type = type;
    this.imageUrl = imageUrl;
    this.thriftStore = thriftStore;
  }

  public Integer getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getCategoryLabel() {
    return categoryLabel;
  }

  public String getType() {
    return type;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Long getLikeCount() {
    return likeCount;
  }

  public Long getCommentCount() {
    return commentCount;
  }

  public ThriftStore getThriftStore() {
    return thriftStore;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public AuthUser getDeletedBy() {
    return deletedBy;
  }

  public String getDeletedReason() {
    return deletedReason;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCategoryLabel(String categoryLabel) {
    this.categoryLabel = categoryLabel;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public void setLikeCount(Long likeCount) {
    this.likeCount = likeCount;
  }

  public void setCommentCount(Long commentCount) {
    this.commentCount = commentCount;
  }

  public void setThriftStore(ThriftStore thriftStore) {
    this.thriftStore = thriftStore;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  public void setDeletedBy(AuthUser deletedBy) {
    this.deletedBy = deletedBy;
  }

  public void setDeletedReason(String deletedReason) {
    this.deletedReason = deletedReason;
  }
}
