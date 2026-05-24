package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.parser.Parser;

/**
 * Utility class for formatting HTML content.
 *
 * <p>Uses Jsoup to pretty-print HTML for more readable snapshots and meaningful diffs. Full
 * documents (starting with {@code <!DOCTYPE} or {@code <html}) are serialized as-is; fragments are
 * parsed with {@code parseBodyFragment} so they are never wrapped in unwanted {@code
 * <html>/<body>} elements.
 */
public final class HtmlFormatter {

  private static final int INDENT_AMOUNT = 2;

  private HtmlFormatter() {
    // Utility class — not instantiable
  }

  /**
   * Pretty-prints the given HTML string with consistent indentation.
   *
   * <p>The HTML is parsed and re-serialized with 2-space indentation. Full documents (starting with
   * {@code <!DOCTYPE} or {@code <html}) are returned as complete documents; fragments are returned
   * as-is without additional wrapper elements.
   *
   * @param html the raw HTML string
   * @return the formatted HTML string, or the original if {@code null} or blank
   */
  public static String prettyPrint(String html) {
    if (html == null || html.isBlank()) {
      return html;
    }
    String trimmed = html.stripLeading();
    boolean isFullDocument =
        trimmed.regionMatches(true, 0, "<!doctype", 0, 9)
            || trimmed.regionMatches(true, 0, "<html", 0, 5);
    if (isFullDocument) {
      Document document = Jsoup.parse(html, "", Parser.htmlParser());
      document.outputSettings().syntax(Syntax.html).indentAmount(INDENT_AMOUNT).outline(false);
      return document.html();
    } else {
      Document document = Jsoup.parseBodyFragment(html);
      document.outputSettings().syntax(Syntax.html).indentAmount(INDENT_AMOUNT).outline(false);
      return document.body().html();
    }
  }
}
