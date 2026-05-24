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
  void prettyPrintAppliesIndentationToFragment() {
    String html = "<div><p>Hello</p></div>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted)
        .isNotNull()
        .contains("\n")
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
  void prettyPrintDoesNotMisclassifyCustomElementStartingWithHtml() {
    String html = "<html-component><span>content</span></html-component>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted)
        .contains("<html-component>")
        .contains("content")
        .doesNotContain("<html>")
        .doesNotContain("<body>");
  }

  @Test
  void prettyPrintHandlesHeadFragment() {
    String html = "<head><title>My Page</title><meta charset=\"utf-8\"></head>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted)
        .isNotNull()
        .isNotEmpty()
        .contains("<title>My Page</title>")
        .doesNotContain("<body>");
  }

  @Test
  void prettyPrintHandlesBomPrefixedDocument() {
    String html = "﻿<!DOCTYPE html><html><body><p>BOM doc</p></body></html>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted).contains("<html>").contains("<p>BOM doc</p>");
  }

  @Test
  void prettyPrintHandlesHtmlCommentBeforeDoctype() {
    String html = "<!-- license -->\n<!DOCTYPE html><html><body><p>Hello</p></body></html>";
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted).contains("<html>").contains("<p>Hello</p>");
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
