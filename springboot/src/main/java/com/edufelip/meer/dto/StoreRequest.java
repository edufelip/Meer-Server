package com.edufelip.meer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;

public class StoreRequest {
  @Size(max = 120)
  private String name;

  @Size(max = 2000)
  private String description;

  @Size(max = 256)
  private String openingHours;

  @Size(max = 512)
  private String addressLine;

  @Size(max = 32)
  private String phone;

  @Email
  @Size(max = 320)
  private String email;

  @Size(max = 120)
  private String neighborhood;

  private Double latitude;
  private Double longitude;

  @Size(max = 10)
  private List<String> categories;

  private Boolean isOnlineStore;

  private String facebook;
  private String instagram;
  private String website;
  private String whatsapp;
  private boolean facebookPresent;
  private boolean instagramPresent;
  private boolean websitePresent;
  private boolean whatsappPresent;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOpeningHours() {
    return openingHours;
  }

  public void setOpeningHours(String openingHours) {
    this.openingHours = openingHours;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public void setAddressLine(String addressLine) {
    this.addressLine = addressLine;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getNeighborhood() {
    return neighborhood;
  }

  public void setNeighborhood(String neighborhood) {
    this.neighborhood = neighborhood;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public Boolean getIsOnlineStore() {
    return isOnlineStore;
  }

  public void setIsOnlineStore(Boolean isOnlineStore) {
    this.isOnlineStore = isOnlineStore;
  }

  public String getFacebook() {
    return facebook;
  }

  public String getInstagram() {
    return instagram;
  }

  public String getWebsite() {
    return website;
  }

  public String getWhatsapp() {
    return whatsapp;
  }

  public boolean isFacebookPresent() {
    return facebookPresent;
  }

  public boolean isInstagramPresent() {
    return instagramPresent;
  }

  public boolean isWebsitePresent() {
    return websitePresent;
  }

  public boolean isWhatsappPresent() {
    return whatsappPresent;
  }

  @JsonProperty("facebook")
  public void setFacebook(String facebook) {
    this.facebook = facebook;
    this.facebookPresent = true;
  }

  @JsonProperty("instagram")
  public void setInstagram(String instagram) {
    this.instagram = instagram;
    this.instagramPresent = true;
  }

  @JsonProperty("website")
  public void setWebsite(String website) {
    this.website = website;
    this.websitePresent = true;
  }

  @JsonProperty("whatsapp")
  public void setWhatsapp(String whatsapp) {
    this.whatsapp = whatsapp;
    this.whatsappPresent = true;
  }
}
