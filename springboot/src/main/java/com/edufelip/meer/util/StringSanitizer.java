package com.edufelip.meer.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class StringSanitizer {
  private StringSanitizer() {}

  public static String sanitize(String value) {
    if (value == null) return null;
    return Jsoup.clean(value, Safelist.none()).trim();
  }

  public static String sanitizeAndTruncate(String value, int maxLength) {
    if (value == null) return null;
    String cleaned = Jsoup.clean(value, Safelist.none()).trim();
    if (cleaned.length() > maxLength) {
      return cleaned.substring(0, maxLength);
    }
    return cleaned;
  }
}
