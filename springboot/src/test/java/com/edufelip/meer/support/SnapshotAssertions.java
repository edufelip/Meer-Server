package com.edufelip.meer.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class SnapshotAssertions {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  private SnapshotAssertions() {}

  public static void assertJsonSnapshot(String resourcePath, String actualJson) throws Exception {
    String expected = readResource(resourcePath);
    if (expected.trim().isEmpty()) {
      assertThat(actualJson == null ? "" : actualJson.trim()).isEmpty();
      return;
    }
    JsonNode expectedNode = MAPPER.readTree(expected);
    JsonNode actualNode = MAPPER.readTree(actualJson);
    assertThat(actualNode).isEqualTo(expectedNode);
  }

  private static String readResource(String resourcePath) throws Exception {
    try (InputStream stream =
        SnapshotAssertions.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IllegalStateException("Missing snapshot resource: " + resourcePath);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
