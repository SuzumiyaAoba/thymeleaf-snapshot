package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlFormatterTest {

  @Test
  void prettyPrintFormatsFullDocumentWithIndentation() {
    String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted).isNotNull().contains("<p>Hello</p>").contains("\n");
  }

  @Test
  void prettyPrintDoesNotWrapFragmentInHtmlBody() {
    String html = "<div>Hello</div>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted)
        .isNotNull()
        .contains("<div>")
        .contains("Hello")
        .doesNotContain("<html>")
        .doesNotContain("<body>");
  }

  @Test
  void prettyPrintHandlesMultiElementFragment() {
    String html = "<p>First</p><p>Second</p>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted)
        .contains("<p>First</p>")
        .contains("<p>Second</p>")
        .doesNotContain("<html>");
  }

  @Test
  void prettyPrintHandlesDoctype() {
    String html = "<!DOCTYPE html><html><body><p>Hello</p></body></html>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted).contains("<p>Hello</p>").contains("<html>");
  }

  @Test
  void prettyPrintReturnsNullForNull() {
    assertThat(HtmlFormatter.prettyPrint(null)).isNull();
  }

  @Test
  void prettyPrintReturnsBlankForBlank() {
    assertThat(HtmlFormatter.prettyPrint("   ")).isEqualTo("   ");
  }
}
