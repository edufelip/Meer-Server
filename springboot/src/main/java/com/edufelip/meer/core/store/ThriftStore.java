package com.edufelip.meer.core.store;

import com.edufelip.meer.core.content.GuideContent;
import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
public class ThriftStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column
    private String tagline;

    @Column
    private String coverImageUrl;

    @ElementCollection
    private List<String> galleryUrls;

    @Column(nullable = false)
    private String addressLine;

    private Double latitude;
    private Double longitude;
    private String mapImageUrl;
    private String phone;
    private String email;

    @Column
    private String openingHours;

    private String openingHoursNotes;

    @Embedded
    private Social social;

    @ElementCollection
    private List<String> categories;

    private Double distanceKm;
    private Integer walkTimeMinutes;
    private String neighborhood;
    private String badgeLabel;
    private Boolean isFavorite;
    private String description;

    @OneToMany(mappedBy = "thriftStore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuideContent> contents;

    @OneToMany(mappedBy = "thriftStore", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ThriftStorePhoto> photos = new ArrayList<>();

    public ThriftStore() {}

    public ThriftStore(Integer id, String name, String tagline, String coverImageUrl, List<String> galleryUrls,
                       String addressLine, Double latitude, Double longitude, String mapImageUrl, String openingHours,
                       String openingHoursNotes, Social social, List<String> categories, Double distanceKm,
                       Integer walkTimeMinutes, String neighborhood, String badgeLabel, Boolean isFavorite,
                       String description, List<GuideContent> contents) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.coverImageUrl = coverImageUrl;
        this.galleryUrls = galleryUrls;
        this.addressLine = addressLine;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapImageUrl = mapImageUrl;
        this.openingHours = openingHours;
        this.openingHoursNotes = openingHoursNotes;
        this.social = social;
        this.categories = categories;
        this.distanceKm = distanceKm;
        this.walkTimeMinutes = walkTimeMinutes;
        this.neighborhood = neighborhood;
        this.badgeLabel = badgeLabel;
        this.isFavorite = isFavorite;
        this.description = description;
        this.contents = contents;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getTagline() { return tagline; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public List<String> getGalleryUrls() { return galleryUrls; }
    public String getAddressLine() { return addressLine; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getMapImageUrl() { return mapImageUrl; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getOpeningHours() { return openingHours; }
    public String getOpeningHoursNotes() { return openingHoursNotes; }
    public Social getSocial() { return social; }
    public List<String> getCategories() { return categories; }
    public Double getDistanceKm() { return distanceKm; }
    public Integer getWalkTimeMinutes() { return walkTimeMinutes; }
    public String getNeighborhood() { return neighborhood; }
    public String getBadgeLabel() { return badgeLabel; }
    public Boolean getIsFavorite() { return isFavorite; }
    public String getDescription() { return description; }
    public List<GuideContent> getContents() { return contents; }
    public List<ThriftStorePhoto> getPhotos() { return photos; }

    public void setId(Integer id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public void setGalleryUrls(List<String> galleryUrls) { this.galleryUrls = galleryUrls; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setMapImageUrl(String mapImageUrl) { this.mapImageUrl = mapImageUrl; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public void setOpeningHoursNotes(String openingHoursNotes) { this.openingHoursNotes = openingHoursNotes; }
    public void setSocial(Social social) { this.social = social; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public void setWalkTimeMinutes(Integer walkTimeMinutes) { this.walkTimeMinutes = walkTimeMinutes; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public void setBadgeLabel(String badgeLabel) { this.badgeLabel = badgeLabel; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
    public void setDescription(String description) { this.description = description; }
    public void setContents(List<GuideContent> contents) { this.contents = contents; }
    public void setPhotos(List<ThriftStorePhoto> photos) { this.photos = photos; }
}
