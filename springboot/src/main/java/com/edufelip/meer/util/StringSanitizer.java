package com.edufelip.meer.util;

import java.text.Normalizer;

public final class StringSanitizer {
  private static final int MAX_CONSECUTIVE_NEWLINES = 3;

  private StringSanitizer() {}

  /**
   * Sanitizes input by removing HTML tags and script content while preserving line breaks.
   * Protects against XSS attacks while maintaining text formatting.
   *
   * @param value the input string to sanitize
   * @return sanitized string with preserved newlines, or null if input is null
   */
  public static String sanitize(String value) {
    if (value == null) return null;

    // Normalize Unicode characters
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);

    // Remove script tags and their content (case-insensitive)
    normalized = normalized.replaceAll("(?i)<\\s*script[^>]*>.*?</\\s*script\\s*>", "");
    normalized = normalized.replaceAll("(?i)<\\s*script[^>]*>", "");

    // Remove all HTML tags but keep the content
    normalized = normalized.replaceAll("<[^>]*>", "");

    // Remove any remaining < or > characters (potential XSS vectors)
    normalized = normalized.replaceAll("[<>]", "");

    // Remove HTML entities that could be used for XSS
    normalized = normalized.replaceAll("&[#\\w]+;", "");

    // Remove control characters EXCEPT newline (\n), carriage return (\r), and tab (\t)
    normalized = normalized.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");

    // Limit excessive consecutive newlines (max 3 in a row)
    normalized = normalized.replaceAll("(\\r?\\n){" + (MAX_CONSECUTIVE_NEWLINES + 1) + ",}", "\n\n\n");

    // Trim leading/trailing whitespace but preserve internal newlines
    return normalized.trim();
  }

  /**
   * Sanitizes input and truncates to maximum length while preserving line breaks.
   *
   * @param value the input string to sanitize
   * @param maxLength maximum allowed length after sanitization
   * @return sanitized and truncated string, or null if input is null
   */
  public static String sanitizeAndTruncate(String value, int maxLength) {
    if (value == null) return null;
    String cleaned = sanitize(value);
    if (cleaned.length() > maxLength) {
      return cleaned.substring(0, maxLength);
    }
    return cleaned;
  }
}
