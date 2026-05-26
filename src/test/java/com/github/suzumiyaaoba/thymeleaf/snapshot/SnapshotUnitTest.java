package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        renderer,
        manager,
        "TC",
        "tm",
        annotation("", tmpl, false),
        prettyPrint,
        globalUpdate,
        false,
        new HashSet<>());
  }

  private Snapshot inlineSnapshotCi(String tmpl) {
    return new Snapshot(
        renderer,
        manager,
        "TC",
        "tm",
        annotation("", tmpl, false),
        false,
        false,
        true,
        new HashSet<>());
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

    var result = s.setVariables(Map.of("msg", "hello"));
    assertThat(result).isNotSameAs(s);

    result.assertMatchesSnapshot();
    assertThat(Files.readString(tempDir.resolve("TC/tm.html"))).contains("hello");
  }

  @Test
  void setVariables_throwsOnNull() {
    var s = inlineSnapshot("<p>x</p>", false, false);
    assertThatThrownBy(() -> s.setVariables(null)).isInstanceOf(NullPointerException.class);
  }

  // --- setLocale ---

  @Test
  void setLocale_returnsNewInstance() {
    var s = inlineSnapshot("<p>x</p>", false, false);
    assertThat(s.setLocale(Locale.ENGLISH)).isNotSameAs(s);
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

    var cleared = s.setVariable("msg", "before").clearVariables().setVariable("msg", "after");
    assertThat(cleared).isNotSameAs(s);

    cleared.assertMatchesSnapshot();
    assertThat(Files.readString(tempDir.resolve("TC/tm.html")))
        .contains("after")
        .doesNotContain("before");
  }

  // --- assertMatchesSnapshot: prettyPrint ---

  @Test
  void assertMatchesSnapshot_prettyPrintsWhenEnabled() throws Exception {
    var s = inlineSnapshot("<div><p th:text=\"${msg}\">x</p></div>", true, false);

    s.setVariable("msg", "pretty").assertMatchesSnapshot();

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
    s.setVariable("msg", "new").assertMatchesSnapshot();

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
            false,
            false,
            new HashSet<>());
    s.setVariable("msg", "new").assertMatchesSnapshot();

    assertThat(Files.readString(snapshotPath)).contains("new");
  }

  // --- assertMatchesSnapshot: mismatch ---

  @Test
  void assertMatchesSnapshot_throwsMismatchWhenDiffers() throws Exception {
    writeExistingSnapshot("<p>expected</p>");

    var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, false);
    var withDifferent = s.setVariable("msg", "different");

    assertThatThrownBy(() -> withDifferent.assertMatchesSnapshot())
        .isInstanceOf(SnapshotMismatchException.class);
  }

  // --- assertMatchesSnapshot: accessedPaths tracking ---

  @Test
  void assertMatchesSnapshot_recordsResolvedPathInAccessedSet() throws Exception {
    Set<Path> accessed = new HashSet<>();
    var s =
        new Snapshot(
            renderer,
            manager,
            "TC",
            "tm",
            annotation("", "<p>hi</p>", false),
            false,
            false,
            false,
            accessed);

    s.assertMatchesSnapshot();

    assertThat(accessed).containsExactly(tempDir.resolve("TC/tm.html"));
  }

  @Test
  void assertMatchesSnapshot_recordsNamedPathInAccessedSet() throws Exception {
    Set<Path> accessed = new HashSet<>();
    var s =
        new Snapshot(
            renderer,
            manager,
            "TC",
            "tm",
            annotation("", "<p>hi</p>", false),
            false,
            false,
            false,
            accessed);

    s.assertMatchesSnapshot("v1");
    s.assertMatchesSnapshot("v2");

    assertThat(accessed)
        .containsExactlyInAnyOrder(
            tempDir.resolve("TC/tm[v1].html"), tempDir.resolve("TC/tm[v2].html"));
  }

  @Test
  void assertMatchesSnapshot_fluentCopiesShareAccessedSet() throws Exception {
    Set<Path> accessed = new HashSet<>();
    var base =
        new Snapshot(
            renderer,
            manager,
            "TC",
            "tm",
            annotation("", "<p th:text=\"${x}\">x</p>", false),
            false,
            false,
            false,
            accessed);

    base.setVariable("x", "a").assertMatchesSnapshot("a");
    base.setVariable("x", "b").assertMatchesSnapshot("b");

    assertThat(accessed).hasSize(2);
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
                    false,
                    false,
                    new HashSet<>()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validateAnnotation_throwsWhenNeitherTemplateNorInlineSpecified() {
    assertThatThrownBy(
            () ->
                new Snapshot(
                    renderer,
                    manager,
                    "TC",
                    "tm",
                    annotation("", "", false),
                    false,
                    false,
                    false,
                    new HashSet<>()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validateAnnotation_throwsWhenTemplateIsBlank() {
    assertThatThrownBy(
            () ->
                new Snapshot(
                    renderer,
                    manager,
                    "TC",
                    "tm",
                    annotation("   ", "", false),
                    false,
                    false,
                    false,
                    new HashSet<>()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validateAnnotation_throwsWhenInlineTemplateIsBlank() {
    assertThatThrownBy(
            () ->
                new Snapshot(
                    renderer,
                    manager,
                    "TC",
                    "tm",
                    annotation("", "\n\t", false),
                    false,
                    false,
                    false,
                    new HashSet<>()))
        .isInstanceOf(IllegalStateException.class);
  }

  // --- assertMatchesSnapshot: CI mode ---

  @Test
  void assertMatchesSnapshot_throwsMissingWhenCiModeAndNoSnapshot() {
    var s = inlineSnapshotCi("<p>hello</p>");

    assertThatThrownBy(s::assertMatchesSnapshot)
        .isInstanceOf(SnapshotMissingException.class)
        .hasMessageContaining("TC/tm.html")
        .hasMessageContaining("CI mode");
  }

  @Test
  void assertMatchesSnapshot_passesWhenCiModeAndSnapshotExists() throws Exception {
    writeExistingSnapshot("<p>hello</p>");

    inlineSnapshotCi("<p>hello</p>").assertMatchesSnapshot();
  }

  @Test
  void assertMatchesSnapshot_createsSnapshotWhenCiModeAndUpdateModeActive() throws Exception {
    var s =
        new Snapshot(
            renderer,
            manager,
            "TC",
            "tm",
            annotation("", "<p>hello</p>", false),
            false,
            true,
            true,
            new HashSet<>());

    s.assertMatchesSnapshot();

    assertThat(Files.exists(tempDir.resolve("TC/tm.html"))).isTrue();
  }
}
