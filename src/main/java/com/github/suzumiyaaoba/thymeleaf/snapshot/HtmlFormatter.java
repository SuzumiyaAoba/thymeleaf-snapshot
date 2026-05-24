package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.parser.Parser;

/**
 * Utility class for formatting HTML content.
 *
 * <p>Uses Jsoup to pretty-print HTML for more readable snapshots and meaningful diffs. Detection
 * strips a leading UTF-8 BOM and HTML comments before classifying the input:
 *
 * <ul>
 *   <li>Full documents ({@code <!DOCTYPE} or {@code <html>}) → returned as complete documents.
 *   <li>{@code <head>} fragments → inner head content (without wrapper elements).
 *   <li>All other fragments → inner body content (without wrapper elements).
 * </ul>
 */
public final class HtmlFormatter {

  private static final int INDENT_AMOUNT = 2;

  private HtmlFormatter() {}

  /**
   * Pretty-prints the given HTML string with consistent indentation.
   *
   * <p>The HTML is parsed and re-serialized with 2-space indentation.
   *
   * @param html the raw HTML string
   * @return the formatted HTML string, or the original if {@code null} or blank
   */
  public static String prettyPrint(String html) {
    if (html == null || html.isBlank()) {
      return html;
    }
    String detected = html.stripLeading();
    if (!detected.isEmpty() && detected.charAt(0) == '﻿') {
      detected = detected.substring(1).stripLeading();
    }
    detected = skipLeadingComments(detected);
    if (detected.regionMatches(true, 0, "<!doctype", 0, 9) || isTagStart(detected, "<html")) {
      Document document = Jsoup.parse(html, "", Parser.htmlParser());
      document.outputSettings().syntax(Syntax.html).indentAmount(INDENT_AMOUNT).outline(false);
      return document.html();
    } else if (isTagStart(detected, "<head")) {
      Document document = Jsoup.parse(html, "", Parser.htmlParser());
      document.outputSettings().syntax(Syntax.html).indentAmount(INDENT_AMOUNT).outline(false);
      return document.head().html();
    } else {
      Document document = Jsoup.parseBodyFragment(html);
      document.outputSettings().syntax(Syntax.html).indentAmount(INDENT_AMOUNT).outline(false);
      return document.body().html();
    }
  }

  private static boolean isTagStart(String s, String tag) {
    if (!s.regionMatches(true, 0, tag, 0, tag.length())) {
      return false;
    }
    if (s.length() == tag.length()) {
      return true;
    }
    char next = s.charAt(tag.length());
    return next == '>'
        || next == ' '
        || next == '\t'
        || next == '\n'
        || next == '\r'
        || next == '/';
  }

  private static String skipLeadingComments(String s) {
    while (s.startsWith("<!--")) {
      int end = s.indexOf("-->");
      if (end < 0) {
        break;
      }
      s = s.substring(end + 3).stripLeading();
    }
    return s;
  }
}
