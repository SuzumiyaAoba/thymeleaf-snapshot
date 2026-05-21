package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlFormatterTest {

    @Test
    void prettyPrintFormatsHtmlWithIndentation() {
        String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";
        String formatted = HtmlFormatter.prettyPrint(html);

        assertThat(formatted)
                .isNotNull()
                .contains("<p>Hello</p>")
                .contains("\n");
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
