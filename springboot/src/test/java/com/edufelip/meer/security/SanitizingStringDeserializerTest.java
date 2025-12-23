package com.edufelip.meer.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

class SanitizingStringDeserializerTest {

  private ObjectMapper mapper(int maxLen) {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new SanitizingStringDeserializer(maxLen));
    return new ObjectMapper().rebuild().addModule(module).build();
  }

  record Payload(String value) {}

  @Test
  void stripsHtmlTagsAndScripts() throws Exception {
    ObjectMapper m = mapper(2048);
    String json = "{\"value\":\"<script>alert(1)</script> hello\"}";
    Payload p = m.readValue(json, Payload.class);
    assertThat(p.value()).isEqualTo("hello");
  }

  @Test
  void enforcesMaxLength() throws Exception {
    ObjectMapper m = mapper(5);
    String json = "{\"value\":\"0123456789\"}";
    Payload p = m.readValue(json, Payload.class);
    assertThat(p.value()).isEqualTo("01234");
  }

  @Test
  void trimsWhitespace() throws Exception {
    ObjectMapper m = mapper(2048);
    String json = "{\"value\":\"   hello   \"}";
    Payload p = m.readValue(json, Payload.class);
    assertThat(p.value()).isEqualTo("hello");
  }

  @Test
  void fuzzedInputsStayTrimmedNoHtmlAndWithinMaxLen() throws Exception {
    ObjectMapper m = mapper(64);
    Random random = new Random(1337);
    for (int i = 0; i < 500; i++) {
      String value = randomString(random, 200);
      String json = "{\"value\":" + quoteJson(value) + "}";
      Payload p = m.readValue(json, Payload.class);
      assertThat(p.value()).isNotNull();
      assertThat(p.value().length()).isLessThanOrEqualTo(64);
      assertThat(p.value()).doesNotContain("<").doesNotContain(">");
    }
  }

  private String randomString(Random random, int maxLen) {
    int len = random.nextInt(maxLen + 1);
    String alphabet =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 <>\"'\\n\\t/;:(){}[]";
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    // sprinkle a script tag occasionally
    if (len > 10 && random.nextBoolean()) {
      sb.insert(0, "<script>alert(1)</script> ");
    }
    return sb.toString();
  }

  private String quoteJson(String value) {
    String escaped =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
            .replace("\r", "\\r");
    return "\"" + escaped + "\"";
  }
}
