package com.edufelip.meer.security;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.password-reset")
public class PasswordResetProperties {
  private String baseUrl = "https://guiabrecho.com.br/reset-password";
  private long ttlMinutes = 30;
  private String from = "";
  private String subject = "Reset your password";

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public long getTtlMinutes() {
    return ttlMinutes;
  }

  public void setTtlMinutes(long ttlMinutes) {
    this.ttlMinutes = ttlMinutes;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String buildResetLink(UUID token) {
    String base = baseUrl == null ? "" : baseUrl.trim();
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (base.isEmpty()) {
      return token.toString();
    }
    return base + "/" + token;
  }
}
