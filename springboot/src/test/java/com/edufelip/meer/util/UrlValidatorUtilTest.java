package com.edufelip.meer.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import java.util.Random;
import org.junit.jupiter.api.Test;

class UrlValidatorUtilTest {

  @Test
  void acceptsHttpAndHttps() {
    assertDoesNotThrow(() -> UrlValidatorUtil.ensureHttpUrl("http://example.com", "site"));
    assertDoesNotThrow(() -> UrlValidatorUtil.ensureHttpUrl("https://example.com/path", "site"));
  }

  @Test
  void rejectsJavascriptScheme() {
    assertThrows(
        IllegalArgumentException.class,
        () -> UrlValidatorUtil.ensureHttpUrl("javascript:alert(1)", "site"));
  }

  @Test
  void rejectsRelativeUrl() {
    assertThrows(
        IllegalArgumentException.class, () -> UrlValidatorUtil.ensureHttpUrl("/foo", "site"));
  }

  @Test
  void fuzzedInvalidSchemesAreRejected() {
    Random random = new Random(2024);
    String[] schemes = {"javascript", "data", "file", "ftp", "mailto", "tel", "blob"};

    for (int i = 0; i < 200; i++) {
      String scheme = schemes[random.nextInt(schemes.length)];
      String url = scheme + ":" + randomPath(random);
      assertThrows(
          IllegalArgumentException.class, () -> UrlValidatorUtil.ensureHttpUrl(url, "site"));
    }
  }

  @Test
  void fuzzedHttpUrlsAreAccepted() {
    Random random = new Random(9001);
    for (int i = 0; i < 200; i++) {
      String host = randomHost(random);
      String path = "/" + randomPath(random);
      String scheme = random.nextBoolean() ? "http" : "https";
      String url = scheme + "://" + host + path;
      assertDoesNotThrow(() -> UrlValidatorUtil.ensureHttpUrl(url, "site"));
    }
  }

  private String randomHost(Random random) {
    String[] tlds = {"com", "net", "org", "io"};
    String label = randomString(random, 3, 10).toLowerCase(Locale.ROOT);
    return label + "." + tlds[random.nextInt(tlds.length)];
  }

  private String randomPath(Random random) {
    return randomString(random, 3, 12) + "/" + randomString(random, 3, 12);
  }

  private String randomString(Random random, int minLen, int maxLen) {
    int len = minLen + random.nextInt(maxLen - minLen + 1);
    String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return sb.toString();
  }
}
