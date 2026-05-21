package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.parser.Parser;

/**
 * Utility class for formatting HTML content.
 *
 * <p>Uses Jsoup to pretty-print HTML for more readable snapshots and meaningful diffs.
 *
 * <p><strong>Note:</strong> Jsoup parses HTML and may modify the structure by auto-completing
 * missing elements (e.g., adding {@code <html>}, {@code <head>}, {@code <body>} tags). When using
 * this with template fragments (partial HTML), the output may include wrapper elements not present
 * in the input.
 */
public final class HtmlFormatter {

  private static final int INDENT_AMOUNT = 2;

  private HtmlFormatter() {
    // Utility class — not instantiable
  }

  /**
   * Pretty-prints the given HTML string with consistent indentation.
   *
   * <p>The HTML is parsed and re-serialized with 2-space indentation. This makes snapshots more
   * readable and produces cleaner diffs when changes occur.
   *
   * <p><strong>Caution:</strong> Jsoup may auto-complete the HTML structure. For example, a
   * fragment {@code <p>Hello</p>} will be wrapped in {@code
   * <html><head></head><body>...</body></html>}.
   *
   * @param html the raw HTML string
   * @return the formatted HTML string, or the original if {@code null} or blank
   */
  public static String prettyPrint(String html) {
    if (html == null || html.isBlank()) {
      return html;
    }
    Document document = Jsoup.parse(html, "", Parser.htmlParser());
    document.outputSettings().syntax(Syntax.html).indentAmount(INDENT_AMOUNT).outline(false);
    return document.html();
  }
}
