package com.edufelip.meer.security;

import com.edufelip.meer.util.StringSanitizer;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Global string sanitizer used on request bodies to strip HTML/JS payloads, trim whitespace, and
 * enforce a maximum length to mitigate XSS/DoS vectors.
 */
public class SanitizingStringDeserializer extends StdScalarDeserializer<String> {

  private final int maxLength;

  public SanitizingStringDeserializer(int maxLength) {
    super(String.class);
    this.maxLength = maxLength;
  }

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
    String value = p.getValueAsString();
    if (value == null) return null;
    return StringSanitizer.sanitizeAndTruncate(value, maxLength);
  }
}
