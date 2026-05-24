package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnapshotUnitTest {

  @TempDir Path tempDir;

  private ThymeleafRenderer renderer;
  private SnapshotManager manager;

  @BeforeEach
  void setUp() {
    renderer = new ThymeleafRenderer("templates/", ".html", "UTF-8");
    manager = new SnapshotManager(tempDir);
  }

  // --- helpers ---

  private static SnapshotTest annotation(String template, String inlineTemplate, boolean update) {
    return new SnapshotTest() {
      @Override
      public String template() {
        return template;
      }

      @Override
      public String inlineTemplate() {
        return inlineTemplate;
      }

      @Override
      public boolean update() {
        return update;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return SnapshotTest.class;
      }
    };
  }

  private Snapshot inlineSnapshot(String tmpl, boolean prettyPrint, boolean globalUpdate) {
    return new Snapshot(
        renderer, manager, "TC", "tm", annotation("", tmpl, false), prettyPrint, globalUpdate);
  }

  private Path writeExistingSnapshot(String content) throws Exception {
    Path snapshotPath = tempDir.resolve("TC/tm.html");
    Files.createDirectories(snapshotPath.getParent());
    Files.writeString(snapshotPath, content);
    return snapshotPath;
  }

  // --- setVariables ---

  @Test
  void setVariables_addsAllEntries() throws Exception {
    var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, false);

    assertThat(s.setVariables(Map.of("msg", "hello"))).isSameAs(s);

    s.assertMatchesSnapshot();
    assertThat(Files.readString(tempDir.resolve("TC/tm.html"))).contains("hello");
  }

  @Test
  void setVariables_throwsOnNull() {
    var s = inlineSnapshot("<p>x</p>", false, false);
    assertThatThrownBy(() -> s.setVariables(null)).isInstanceOf(NullPointerException.class);
  }

  // --- setLocale ---

  @Test
  void setLocale_returnsThis() {
    var s = inlineSnapshot("<p>x</p>", false, false);
    assertThat(s.setLocale(Locale.ENGLISH)).isSameAs(s);
  }

  @Test
  void setLocale_throwsOnNull() {
    var s = inlineSnapshot("<p>x</p>", false, false);
    assertThatThrownBy(() -> s.setLocale(null)).isInstanceOf(NullPointerException.class);
  }

  // --- clearVariables ---

  @Test
  void clearVariables_removesAllVariables() throws Exception {
    var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, false);
    s.setVariable("msg", "before");

    assertThat(s.clearVariables()).isSameAs(s);

    s.setVariable("msg", "after");
    s.assertMatchesSnapshot();
    assertThat(Files.readString(tempDir.resolve("TC/tm.html")))
        .contains("after")
        .doesNotContain("before");
  }

  // --- assertMatchesSnapshot: prettyPrint ---

  @Test
  void assertMatchesSnapshot_prettyPrintsWhenEnabled() throws Exception {
    var s = inlineSnapshot("<div><p th:text=\"${msg}\">x</p></div>", true, false);
    s.setVariable("msg", "pretty");

    s.assertMatchesSnapshot();

    assertThat(Files.readString(tempDir.resolve("TC/tm.html")))
        .as("prettyPrint should add newlines and not wrap in html/body")
        .contains("\n")
        .doesNotContain("<html>")
        .doesNotContain("<body>");
  }

  // --- assertMatchesSnapshot: update mode ---

  @Test
  void assertMatchesSnapshot_updatesExistingSnapshotWhenGlobalUpdateTrue() throws Exception {
    Path snapshotPath = writeExistingSnapshot("old content");

    var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, true);
    s.setVariable("msg", "new");
    s.assertMatchesSnapshot(); // should overwrite, not throw

    assertThat(Files.readString(snapshotPath)).contains("new").doesNotContain("old content");
  }

  @Test
  void assertMatchesSnapshot_updatesExistingSnapshotWhenAnnotationUpdateTrue() throws Exception {
    Path snapshotPath = writeExistingSnapshot("old content");

    var s =
        new Snapshot(
            renderer,
            manager,
            "TC",
            "tm",
            annotation("", "<p th:text=\"${msg}\">x</p>", true),
            false,
            false);
    s.setVariable("msg", "new");
    s.assertMatchesSnapshot();

    assertThat(Files.readString(snapshotPath)).contains("new");
  }

  // --- assertMatchesSnapshot: mismatch ---

  @Test
  void assertMatchesSnapshot_throwsMismatchWhenDiffers() throws Exception {
    writeExistingSnapshot("<p>expected</p>");

    var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, false);
    s.setVariable("msg", "different");

    assertThatThrownBy(() -> s.assertMatchesSnapshot())
        .isInstanceOf(SnapshotMismatchException.class);
  }

  // --- validateAnnotation ---

  @Test
  void validateAnnotation_throwsWhenBothTemplateAndInlineSpecified() {
    assertThatThrownBy(
            () ->
                new Snapshot(
                    renderer,
                    manager,
                    "TC",
                    "tm",
                    annotation("tmpl", "inline", false),
                    false,
                    false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validateAnnotation_throwsWhenNeitherTemplateNorInlineSpecified() {
    assertThatThrownBy(
            () ->
                new Snapshot(
                    renderer, manager, "TC", "tm", annotation("", "", false), false, false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validateAnnotation_throwsWhenTemplateIsBlank() {
    assertThatThrownBy(
            () ->
                new Snapshot(
                    renderer, manager, "TC", "tm", annotation("   ", "", false), false, false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validateAnnotation_throwsWhenInlineTemplateIsBlank() {
    assertThatThrownBy(
            () ->
                new Snapshot(
                    renderer, manager, "TC", "tm", annotation("", "\n\t", false), false, false))
        .isInstanceOf(IllegalStateException.class);
  }
}
