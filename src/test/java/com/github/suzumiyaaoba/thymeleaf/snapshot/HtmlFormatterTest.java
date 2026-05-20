package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlFormatterTest {

    @Test
    void prettyPrintFormatsHtmlWithIndentation() {
        String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";
        String formatted = HtmlFormatter.prettyPrint(html);

        assertNotNull(formatted);
        assertTrue(formatted.contains("<p>Hello</p>"));
        // Formatted should have newlines
        assertTrue(formatted.contains("\n"));
    }

    @Test
    void prettyPrintReturnsNullForNull() {
        assertNull(HtmlFormatter.prettyPrint(null));
    }

    @Test
    void prettyPrintReturnsBlankForBlank() {
        String result = HtmlFormatter.prettyPrint("   ");
        assertEquals("   ", result);
    }
}
