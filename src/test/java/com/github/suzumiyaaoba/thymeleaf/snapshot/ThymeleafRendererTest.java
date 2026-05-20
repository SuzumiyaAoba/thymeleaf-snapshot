package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThymeleafRendererTest {

    @Test
    void renderClasspathTemplate() {
        ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
        String result = renderer.render("simple", Map.of("title", "Test Title"), Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.contains("Test Title"));
        assertTrue(result.contains("<h1>Test Title</h1>"));
    }

    @Test
    void renderClasspathTemplateWithVariables() {
        ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
        Map<String, Object> variables = Map.of(
                "pageTitle", "My Page",
                "heading", "Welcome",
                "message", "Hello World",
                "items", List.of("Alpha", "Beta", "Gamma")
        );
        String result = renderer.render("variables", variables, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.contains("My Page"));
        assertTrue(result.contains("Welcome"));
        assertTrue(result.contains("Hello World"));
        assertTrue(result.contains("Alpha"));
        assertTrue(result.contains("Beta"));
        assertTrue(result.contains("Gamma"));
    }

    @Test
    void renderInlineTemplate() {
        ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
        String template = "<p th:text=\"${message}\">placeholder</p>";
        String result = renderer.renderInline(template, Map.of("message", "Hello!"), Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.contains("Hello!"));
        assertFalse(result.contains("placeholder"));
    }

    @Test
    void renderInlineTemplateWithEmptyVariables() {
        ThymeleafRenderer renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
        String template = "<p>Static Content</p>";
        String result = renderer.renderInline(template, Map.of(), Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.contains("Static Content"));
    }
}
