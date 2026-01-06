package com.edufelip.meer.core.store;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Social {
  @Column(length = 2048)
  private String facebook;

  @Column(length = 2048)
  private String instagram;

  @Column(length = 2048)
  private String website;

  @Column(length = 2048)
  private String whatsapp;

  public Social() {}

  public Social(String facebook, String instagram, String website) {
    this(facebook, instagram, website, null);
  }

  public Social(String facebook, String instagram, String website, String whatsapp) {
    this.facebook = facebook;
    this.instagram = instagram;
    this.website = website;
    this.whatsapp = whatsapp;
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

  public void setFacebook(String facebook) {
    this.facebook = facebook;
  }

  public void setInstagram(String instagram) {
    this.instagram = instagram;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public void setWhatsapp(String whatsapp) {
    this.whatsapp = whatsapp;
  }
}
