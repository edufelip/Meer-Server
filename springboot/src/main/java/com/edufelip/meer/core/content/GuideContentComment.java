package com.edufelip.meer.core.content;

import com.edufelip.meer.core.auth.AuthUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "guide_content_comment")
public class GuideContentComment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "auth_user_id", columnDefinition = "uuid", nullable = false)
  private AuthUser user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false)
  private GuideContent content;

  @Column(nullable = false, length = 120)
  private String body;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "edited_at")
  private Instant editedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "edited_by_user_id", columnDefinition = "uuid")
  private AuthUser editedBy;

  public GuideContentComment() {}

  public GuideContentComment(AuthUser user, GuideContent content, String body) {
    this.user = user;
    this.content = content;
    this.body = body;
  }

  public Integer getId() {
    return id;
  }

  public AuthUser getUser() {
    return user;
  }

  public GuideContent getContent() {
    return content;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getEditedAt() {
    return editedAt;
  }

  public AuthUser getEditedBy() {
    return editedBy;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setUser(AuthUser user) {
    this.user = user;
  }

  public void setContent(GuideContent content) {
    this.content = content;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setEditedAt(Instant editedAt) {
    this.editedAt = editedAt;
  }

  public void setEditedBy(AuthUser editedBy) {
    this.editedBy = editedBy;
  }

}
