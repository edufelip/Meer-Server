package com.edufelip.meer.core.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category")
public class Category {
    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "image_res_id", nullable = false)
    private String imageResId;

    public Category() {}

    public Category(String id, String name, String imageResId) {
        this.id = id;
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getImageResId() { return imageResId; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setImageResId(String imageResId) { this.imageResId = imageResId; }
}
