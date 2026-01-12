package com.edufelip.meer.core.store;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.util.Uuid7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class ThriftStore {
  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(length = 2048)
  private String tagline;

  @Column(length = 2048)
  private String coverImageUrl;

  @ElementCollection
  @Column(length = 2048)
  private List<String> galleryUrls;

  @Column(nullable = false)
  private String addressLine;

  private Double latitude;
  private Double longitude;
  private String phone;
  private String email;

  @Column private String openingHours;

  @Embedded private Social social;

  @ElementCollection(fetch = FetchType.LAZY)
  @org.hibernate.annotations.BatchSize(size = 50)
  private List<String> categories;

  private String neighborhood;
  private String badgeLabel;

  @Column(length = 1000)
  private String description;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "thriftStore", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 25)
  private List<GuideContent> contents;

  @OneToMany(mappedBy = "thriftStore", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC, id ASC")
  @BatchSize(size = 25)
  private List<ThriftStorePhoto> photos = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "owner_id", columnDefinition = "uuid")
  private AuthUser owner;

  public ThriftStore() {}

  public ThriftStore(
      UUID id,
      String name,
      String tagline,
      String coverImageUrl,
      List<String> galleryUrls,
      String addressLine,
      Double latitude,
      Double longitude,
      String openingHours,
      Social social,
      List<String> categories,
      String neighborhood,
      String badgeLabel,
      String description,
      List<GuideContent> contents) {
    this.id = id;
    this.name = name;
    this.tagline = tagline;
    this.coverImageUrl = coverImageUrl;
    this.galleryUrls = galleryUrls;
    this.addressLine = addressLine;
    this.latitude = latitude;
    this.longitude = longitude;
    this.openingHours = openingHours;
    this.social = social;
    this.categories = categories;
    this.neighborhood = neighborhood;
    this.badgeLabel = badgeLabel;
    this.description = description;
    this.contents = contents;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTagline() {
    return tagline;
  }

  public String getCoverImageUrl() {
    return coverImageUrl;
  }

  public List<String> getGalleryUrls() {
    return galleryUrls;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public String getOpeningHours() {
    return openingHours;
  }

  public Social getSocial() {
    return social;
  }

  public List<String> getCategories() {
    return categories;
  }

  public String getNeighborhood() {
    return neighborhood;
  }

  public String getBadgeLabel() {
    return badgeLabel;
  }

  public String getDescription() {
    return description;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<GuideContent> getContents() {
    return contents;
  }

  public List<ThriftStorePhoto> getPhotos() {
    return photos;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTagline(String tagline) {
    this.tagline = tagline;
  }

  public void setCoverImageUrl(String coverImageUrl) {
    this.coverImageUrl = coverImageUrl;
  }

  public void setGalleryUrls(List<String> galleryUrls) {
    this.galleryUrls = galleryUrls;
  }

  public void setAddressLine(String addressLine) {
    this.addressLine = addressLine;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setOpeningHours(String openingHours) {
    this.openingHours = openingHours;
  }

  public void setSocial(Social social) {
    this.social = social;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public void setNeighborhood(String neighborhood) {
    this.neighborhood = neighborhood;
  }

  public void setBadgeLabel(String badgeLabel) {
    this.badgeLabel = badgeLabel;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setContents(List<GuideContent> contents) {
    this.contents = contents;
  }

  public void setPhotos(List<ThriftStorePhoto> photos) {
    this.photos = photos;
  }

  public com.edufelip.meer.core.auth.AuthUser getOwner() {
    return owner;
  }

  public void setOwner(com.edufelip.meer.core.auth.AuthUser owner) {
    this.owner = owner;
  }

  @PrePersist
  public void ensureId() {
    if (this.id == null) {
      this.id = Uuid7.next();
    }
  }
}
