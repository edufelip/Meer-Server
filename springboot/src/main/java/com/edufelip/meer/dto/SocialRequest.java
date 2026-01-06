package com.edufelip.meer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SocialRequest {
  private String facebook;
  private String instagram;
  private String website;
  private String whatsapp;
  private boolean facebookPresent;
  private boolean instagramPresent;
  private boolean websitePresent;
  private boolean whatsappPresent;

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
