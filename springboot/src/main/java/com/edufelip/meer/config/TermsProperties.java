package com.edufelip.meer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "terms")
public class TermsProperties {
  private String requiredVersion;
  private String url;

  public String getRequiredVersion() {
    return requiredVersion;
  }

  public void setRequiredVersion(String requiredVersion) {
    this.requiredVersion = requiredVersion;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String requiredVersionOrNull() {
    return StringUtils.hasText(requiredVersion) ? requiredVersion : null;
  }

  public String urlOrNull() {
    return StringUtils.hasText(url) ? url : null;
  }
}
