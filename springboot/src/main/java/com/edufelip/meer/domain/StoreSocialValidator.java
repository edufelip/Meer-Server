package com.edufelip.meer.domain;

import com.edufelip.meer.util.UrlValidatorUtil;

public final class StoreSocialValidator {
  private StoreSocialValidator() {}

  public static void validate(String facebook, String instagram, String website) {
    ensureWebsiteContainsDotCom(website);
    UrlValidatorUtil.ensureHttpUrl(facebook, "facebook");
    ensureSingleWord(instagram, "instagram");
  }

  private static void ensureWebsiteContainsDotCom(String website) {
    if (website == null) return;
    String trimmed = website.trim();
    if (trimmed.isBlank()) return;
    if (!trimmed.contains(".com")) {
      throw new IllegalArgumentException("website must contain .com");
    }
  }

  private static void ensureSingleWord(String value, String fieldName) {
    if (value == null || value.isBlank()) return;
    String trimmed = value.trim();
    for (int i = 0; i < trimmed.length(); i++) {
      if (Character.isWhitespace(trimmed.charAt(i))) {
        throw new IllegalArgumentException(fieldName + " must be a single word");
      }
    }
  }
}
