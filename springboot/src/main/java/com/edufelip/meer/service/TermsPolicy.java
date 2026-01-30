package com.edufelip.meer.service;

import com.edufelip.meer.config.TermsProperties;
import org.springframework.stereotype.Component;

@Component
public class TermsPolicy {
  private final TermsProperties termsProperties;

  public TermsPolicy(TermsProperties termsProperties) {
    this.termsProperties = termsProperties;
  }

  public String requiredVersionOrNull() {
    return termsProperties.requiredVersionOrNull();
  }

  public String urlOrNull() {
    return termsProperties.urlOrNull();
  }
}
