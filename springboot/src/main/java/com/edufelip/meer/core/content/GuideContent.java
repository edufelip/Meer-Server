package com.edufelip.meer.core.content;

import com.edufelip.meer.core.store.ThriftStore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thrift_store_id", columnDefinition = "uuid")
    private ThriftStore thriftStore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GuideContent() {}

    public GuideContent(Integer id, String title, String description, String categoryLabel, String type, String imageUrl, ThriftStore thriftStore) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryLabel = categoryLabel;
        this.type = type;
        this.imageUrl = imageUrl;
        this.thriftStore = thriftStore;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getType() { return type; }
    public String getImageUrl() { return imageUrl; }
    public ThriftStore getThriftStore() { return thriftStore; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Integer id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
    public void setType(String type) { this.type = type; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setThriftStore(ThriftStore thriftStore) { this.thriftStore = thriftStore; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
