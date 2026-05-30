package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class HtmlFormatterTest {

  static Stream<Arguments> prettyPrintCases() {
    return Stream.of(
        arguments(
            "full document keeps content and indentation",
            "<html><head><title>Test</title></head><body><p>Hello</p></body></html>",
            List.of("<p>Hello</p>", "\n"),
            List.of()),
        arguments(
            "fragment is not wrapped in html/body",
            "<div>Hello</div>",
            List.of("<div>", "Hello"),
            List.of("<html>", "<body>")),
        arguments(
            "fragment receives indentation",
            "<div><p>Hello</p></div>",
            List.of("\n"),
            List.of("<html>", "<body>")),
        arguments(
            "multi-element fragment keeps every element",
            "<p>First</p><p>Second</p>",
            List.of("<p>First</p>", "<p>Second</p>"),
            List.of("<html>")),
        arguments(
            "doctype is treated as a full document",
            "<!DOCTYPE html><html><body><p>Hello</p></body></html>",
            List.of("<p>Hello</p>", "<html>"),
            List.of()),
        arguments(
            "custom element starting with 'html' is not misclassified",
            "<html-component><span>content</span></html-component>",
            List.of("<html-component>", "content"),
            List.of("<html>", "<body>")),
        arguments(
            "head fragment keeps head content only",
            "<head><title>My Page</title><meta charset=\"utf-8\"></head>",
            List.of("<title>My Page</title>"),
            List.of("<body>")),
        arguments(
            "BOM prefix is stripped before classification",
            "﻿<!DOCTYPE html><html><body><p>BOM doc</p></body></html>",
            List.of("<html>", "<p>BOM doc</p>"),
            List.of()),
        arguments(
            "leading comment before doctype is skipped",
            "<!-- license -->\n<!DOCTYPE html><html><body><p>Hello</p></body></html>",
            List.of("<html>", "<p>Hello</p>"),
            List.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prettyPrintCases")
  void prettyPrintProducesExpectedFragments(
      String description, String html, List<String> mustContain, List<String> mustNotContain) {
    String formatted = HtmlFormatter.prettyPrint(html);

    assertThat(formatted).as(description).isNotNull();
    for (String fragment : mustContain) {
      assertThat(formatted).as("%s: should contain '%s'", description, fragment).contains(fragment);
    }
    for (String fragment : mustNotContain) {
      assertThat(formatted)
          .as("%s: should not contain '%s'", description, fragment)
          .doesNotContain(fragment);
    }
  }

  @ParameterizedTest(name = "returns input unchanged for [{0}]")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\n\t "})
  void prettyPrintReturnsInputUnchangedWhenNullOrBlank(String input) {
    assertThat(HtmlFormatter.prettyPrint(input)).isEqualTo(input);
  }
}
