package com.edufelip.meer.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringSanitizerTest {

  @Test
  void preservesNewlines() {
    String input = "Line 1\nLine 2\nLine 3";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Line 1\nLine 2\nLine 3");
    assertThat(result).contains("\n");
  }

  @Test
  void preservesCarriageReturnAndNewline() {
    String input = "Line 1\r\nLine 2\r\nLine 3";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).contains("Line 1");
    assertThat(result).contains("Line 2");
    assertThat(result).contains("Line 3");
  }

  @Test
  void preservesTabs() {
    String input = "Column1\tColumn2\tColumn3";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Column1\tColumn2\tColumn3");
    assertThat(result).contains("\t");
  }

  @Test
  void removesScriptTags() {
    String input = "<script>alert('XSS')</script>Hello World";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Hello World");
    assertThat(result).doesNotContain("<script>");
    assertThat(result).doesNotContain("alert");
  }

  @Test
  void removesScriptTagsCaseInsensitive() {
    String input = "<SCRIPT>alert(1)</SCRIPT>Test<ScRiPt>bad()</sCrIpT>";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Test");
    assertThat(result).doesNotContain("alert");
    assertThat(result).doesNotContain("bad");
  }

  @Test
  void removesScriptTagsWithAttributes() {
    String input = "<script type=\"text/javascript\" src=\"evil.js\">alert(1)</script>Safe";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Safe");
  }

  @Test
  void removesHtmlTags() {
    String input = "<div>Hello</div><p>World</p>";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("HelloWorld");
  }

  @Test
  void removesHtmlTagsButKeepsContent() {
    String input = "<b>Bold</b> and <i>italic</i> text";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Bold and italic text");
  }

  @Test
  void removesHtmlEntities() {
    String input = "Hello&nbsp;World&amp;test";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("HelloWorldtest");
    assertThat(result).doesNotContain("&");
  }

  @Test
  void removesControlCharacters() {
    String input = "Hello\u0000World\u0001Test";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("HelloWorldTest");
  }

  @Test
  void limitsConsecutiveNewlines() {
    String input = "Line 1\n\n\n\n\n\n\n\nLine 2";
    String result = StringSanitizer.sanitize(input);
    // Should be limited to max 3 consecutive newlines
    assertThat(result).isEqualTo("Line 1\n\n\nLine 2");
  }

  @Test
  void trimsLeadingAndTrailingWhitespace() {
    String input = "   Hello World   ";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Hello World");
  }

  @Test
  void trimsLeadingAndTrailingNewlines() {
    String input = "\n\n\nHello\nWorld\n\n\n";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Hello\nWorld");
  }

  @Test
  void handlesNullInput() {
    String result = StringSanitizer.sanitize(null);
    assertThat(result).isNull();
  }

  @Test
  void handlesEmptyString() {
    String result = StringSanitizer.sanitize("");
    assertThat(result).isEmpty();
  }

  @Test
  void preservesNewlinesInMultiParagraphText() {
    String input = "Paragraph 1 is here.\n\nParagraph 2 follows.\n\nParagraph 3 concludes.";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo(input);
  }

  @Test
  void combinesXssRemovalWithNewlinePreservation() {
    String input = "Line 1\n<script>alert(1)</script>\nLine 2\n<b>Bold</b>";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Line 1\n\nLine 2\nBold");
    assertThat(result).contains("\n");
    assertThat(result).doesNotContain("<");
  }

  @Test
  void sanitizeAndTruncatePreservesNewlines() {
    String input = "Line 1\nLine 2\nLine 3 is very long and should be truncated";
    String result = StringSanitizer.sanitizeAndTruncate(input, 20);
    assertThat(result).hasSize(20);
    assertThat(result).contains("\n");
    assertThat(result).startsWith("Line 1\nLine 2\nLine ");
  }

  @Test
  void sanitizeAndTruncateHandlesNullInput() {
    String result = StringSanitizer.sanitizeAndTruncate(null, 100);
    assertThat(result).isNull();
  }

  @Test
  void sanitizeAndTruncateWithNoTruncationNeeded() {
    String input = "Short\nText";
    String result = StringSanitizer.sanitizeAndTruncate(input, 100);
    assertThat(result).isEqualTo("Short\nText");
  }

  @Test
  void normalizesUnicodeCharacters() {
    String input = "Café"; // Unicode normalization test
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isNotNull();
    assertThat(result).contains("Caf");
  }

  @Test
  void handlesComplexXssAttempts() {
    String input = "<img src=x onerror=alert(1)>Text<iframe src=\"evil\"></iframe>";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Text");
    assertThat(result).doesNotContain("<");
    assertThat(result).doesNotContain("alert");
    assertThat(result).doesNotContain("onerror");
  }

  @Test
  void handlesNestedScriptTags() {
    String input = "<script><script>alert(1)</script></script>Safe";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).isEqualTo("Safe");
  }

  @Test
  void preservesMultipleTypesOfWhitespace() {
    String input = "Line 1\nLine 2\tTabbed\rLine 3";
    String result = StringSanitizer.sanitize(input);
    assertThat(result).contains("\n");
    assertThat(result).contains("\t");
  }
}
