package com.edufelip.meer.core.content;

import com.edufelip.meer.core.store.ThriftStore;
import jakarta.persistence.*;

@Entity
public class GuideContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String categoryLabel;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thrift_store_id")
    private ThriftStore thriftStore;

    public GuideContent() {}

    public GuideContent(Integer id, String title, String description, String categoryLabel, String imageUrl, ThriftStore thriftStore) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryLabel = categoryLabel;
        this.imageUrl = imageUrl;
        this.thriftStore = thriftStore;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getImageUrl() { return imageUrl; }
    public ThriftStore getThriftStore() { return thriftStore; }

    public void setId(Integer id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setThriftStore(ThriftStore thriftStore) { this.thriftStore = thriftStore; }
}
