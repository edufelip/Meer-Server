package com.edufelip.meer.dto;

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

  @Size(max = 280)
  private String tagline;

  private Double latitude;
  private Double longitude;

  @Size(max = 10)
  private List<String> categories;

  private SocialRequest social;

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

  public String getTagline() {
    return tagline;
  }

  public void setTagline(String tagline) {
    this.tagline = tagline;
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

  public SocialRequest getSocial() {
    return social;
  }

  public void setSocial(SocialRequest social) {
    this.social = social;
  }
}
