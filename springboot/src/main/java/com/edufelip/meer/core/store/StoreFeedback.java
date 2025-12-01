package com.edufelip.meer.core.store;

import com.edufelip.meer.core.auth.AuthUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "store_feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"auth_user_id", "thrift_store_id"}))
public class StoreFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "auth_user_id", columnDefinition = "uuid")
    private AuthUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "thrift_store_id", columnDefinition = "uuid")
    private ThriftStore thriftStore;

    @Column(nullable = true)
    private Integer score; // 1-5, nullable when only review text is provided

    @Column(columnDefinition = "text")
    private String body;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public StoreFeedback() {}

    public StoreFeedback(AuthUser user, ThriftStore thriftStore, Integer score, String body) {
        this.user = user;
        this.thriftStore = thriftStore;
        this.score = score;
        this.body = body;
    }

    public Integer getId() { return id; }
    public AuthUser getUser() { return user; }
    public ThriftStore getThriftStore() { return thriftStore; }
    public Integer getScore() { return score; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Integer id) { this.id = id; }
    public void setUser(AuthUser user) { this.user = user; }
    public void setThriftStore(ThriftStore thriftStore) { this.thriftStore = thriftStore; }
    public void setScore(Integer score) { this.score = score; }
    public void setBody(String body) { this.body = body; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
