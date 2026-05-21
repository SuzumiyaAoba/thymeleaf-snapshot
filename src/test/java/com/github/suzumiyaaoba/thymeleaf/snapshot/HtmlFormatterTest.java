package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlFormatterTest {

  @Test
  void prettyPrintFormatsHtmlWithIndentation() {
    String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted).isNotNull().contains("<p>Hello</p>").contains("\n");
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
