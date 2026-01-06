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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "guide_content_like",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"auth_user_id", "content_id"})})
public class GuideContentLike {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "auth_user_id", columnDefinition = "uuid", nullable = false)
  private AuthUser user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false)
  private GuideContent content;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  public GuideContentLike() {}

  public GuideContentLike(AuthUser user, GuideContent content) {
    this.user = user;
    this.content = content;
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

  public Instant getCreatedAt() {
    return createdAt;
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

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
