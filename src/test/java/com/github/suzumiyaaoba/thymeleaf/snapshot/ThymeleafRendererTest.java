package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThymeleafRendererTest {

  @Test
  void renderClasspathTemplate() {
    ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
    String result = renderer.render("simple", Map.of("title", "Test Title"), Locale.ENGLISH);

    assertThat(result).isNotNull().contains("Test Title").contains("<h1>Test Title</h1>");
  }

  @Test
  void renderClasspathTemplateWithVariables() {
    ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
    Map<String, Object> variables =
        Map.of(
            "pageTitle", "My Page",
            "heading", "Welcome",
            "message", "Hello World",
            "items", List.of("Alpha", "Beta", "Gamma"));
    String result = renderer.render("variables", variables, Locale.ENGLISH);

    assertThat(result)
        .isNotNull()
        .contains("My Page", "Welcome", "Hello World", "Alpha", "Beta", "Gamma");
  }

  @Test
  void renderInlineTemplate() {
    ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
    String template = "<p th:text=\"${message}\">placeholder</p>";
    String result = renderer.renderInline(template, Map.of("message", "Hello!"), Locale.ENGLISH);

    assertThat(result).isNotNull().contains("Hello!").doesNotContain("placeholder");
  }

  @Test
  void renderInlineTemplateWithEmptyVariables() {
    ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
    String template = "<p>Static Content</p>";
    String result = renderer.renderInline(template, Map.of(), Locale.ENGLISH);

    assertThat(result).isNotNull().contains("Static Content");
  }
}
