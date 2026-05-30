package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ThymeleafRendererTest {

  private final ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");

  static Stream<Arguments> classpathTemplateCases() {
    return Stream.of(
        arguments(
            "simple", Map.of("title", "Test Title"), List.of("Test Title", "<h1>Test Title</h1>")),
        arguments(
            "variables",
            Map.of(
                "pageTitle", "My Page",
                "heading", "Welcome",
                "message", "Hello World",
                "items", List.of("Alpha", "Beta", "Gamma")),
            List.of("My Page", "Welcome", "Hello World", "Alpha", "Beta", "Gamma")));
  }

  @ParameterizedTest(name = "render({0})")
  @MethodSource("classpathTemplateCases")
  void renderClasspathTemplate(
      String template, Map<String, Object> variables, List<String> expectedFragments) {
    String result = renderer.render(template, variables, Locale.ENGLISH);

    assertThat(result).isNotNull();
    for (String fragment : expectedFragments) {
      assertThat(result).contains(fragment);
    }
  }

  static Stream<Arguments> inlineTemplateCases() {
    return Stream.of(
        arguments(
            "<p th:text=\"${message}\">placeholder</p>",
            Map.of("message", "Hello!"),
            List.of("Hello!"),
            List.of("placeholder")),
        arguments("<p>Static Content</p>", Map.of(), List.of("Static Content"), List.of()));
  }

  @ParameterizedTest(name = "renderInline[{index}]: {0}")
  @MethodSource("inlineTemplateCases")
  void renderInlineTemplate(
      String template,
      Map<String, Object> variables,
      List<String> mustContain,
      List<String> mustNotContain) {
    String result = renderer.renderInline(template, variables, Locale.ENGLISH);

    assertThat(result).isNotNull();
    for (String fragment : mustContain) {
      assertThat(result).contains(fragment);
    }
    for (String fragment : mustNotContain) {
      assertThat(result).doesNotContain(fragment);
    }
  }
}
