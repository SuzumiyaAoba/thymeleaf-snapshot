package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for the Thymeleaf Snapshot Testing extension.
 *
 * <p>These tests verify the complete workflow: template rendering, snapshot creation, comparison,
 * and update.
 */
@ExtendWith(ThymeleafSnapshotExtension.class)
class ThymeleafSnapshotExtensionTest {

  @TempDir static Path baseDir;

  private static String previousBaseDir;

  @BeforeAll
  static void useTemporarySnapshotDirectory() {
    previousBaseDir = System.setProperty(SnapshotProperties.BASE_DIR, baseDir.toString());
  }

  @AfterAll
  static void restoreSnapshotDirectory() {
    if (previousBaseDir == null) {
      System.clearProperty(SnapshotProperties.BASE_DIR);
    } else {
      System.setProperty(SnapshotProperties.BASE_DIR, previousBaseDir);
    }
  }

  @Test
  void nonSnapshotTestMethodIsIgnoredByExtension() {
    // beforeEach's snapshotTest == null branch is exercised for plain @Test methods
  }

  @Test
  void resolveSnapshotConfigReturnsNullWhenAbsent() {
    class NoConfig {}
    assertThat(ThymeleafSnapshotExtension.resolveSnapshotConfig(NoConfig.class)).isNull();
  }

  @Test
  void resolveSnapshotConfigFindsAnnotationOnDirectClass() {
    @SnapshotConfig(snapshotDir = "custom")
    class Annotated {}
    SnapshotConfig cfg = ThymeleafSnapshotExtension.resolveSnapshotConfig(Annotated.class);
    assertThat(cfg).isNotNull();
    assertThat(cfg.snapshotDir()).isEqualTo("custom");
  }

  @Test
  void resolveSnapshotConfigWalksEnclosingClassChain() {
    @SnapshotConfig(snapshotDir = "from-outer")
    class Outer {
      class Inner {}
    }
    SnapshotConfig cfg = ThymeleafSnapshotExtension.resolveSnapshotConfig(Outer.Inner.class);
    assertThat(cfg).isNotNull();
    assertThat(cfg.snapshotDir()).isEqualTo("from-outer");
  }

  @Test
  void resolveSnapshotConfigPrefersInnermostAnnotation() {
    @SnapshotConfig(snapshotDir = "outer")
    class Outer {
      @SnapshotConfig(snapshotDir = "inner")
      class Inner {}
    }
    SnapshotConfig cfg = ThymeleafSnapshotExtension.resolveSnapshotConfig(Outer.Inner.class);
    assertThat(cfg).isNotNull();
    assertThat(cfg.snapshotDir()).isEqualTo("inner");
  }

  @SnapshotTest(template = "simple")
  void shouldCreateSnapshotOnFirstRun(Snapshot snapshot) {
    snapshot.setVariable("title", "Hello World").assertMatchesSnapshot();

    assertThat(snapshotPath("shouldCreateSnapshotOnFirstRun"))
        .content()
        .contains("<h1>Hello World</h1>");
  }

  @SnapshotTest(inlineTemplate = "<p th:text=\"${message}\">placeholder</p>")
  void shouldRenderInlineTemplate(Snapshot snapshot) {
    snapshot.setVariable("message", "Inline Test").assertMatchesSnapshot();

    assertThat(snapshotPath("shouldRenderInlineTemplate"))
        .content()
        .isEqualTo("<p>Inline Test</p>");
  }

  @SnapshotTest(template = "variables")
  void shouldRenderTemplateWithMultipleVariables(Snapshot snapshot) {
    snapshot
        .setVariable("pageTitle", "Test Page")
        .setVariable("heading", "Welcome")
        .setVariable("message", "Hello from test")
        .setVariable("items", List.of("Item 1", "Item 2", "Item 3"))
        .assertMatchesSnapshot();

    assertThat(snapshotPath("shouldRenderTemplateWithMultipleVariables"))
        .content()
        .contains(
            "<title>Test Page</title>",
            "<h1>Welcome</h1>",
            "<p>Hello from test</p>",
            "<li>Item 1</li>",
            "<li>Item 2</li>",
            "<li>Item 3</li>");
  }

  @SnapshotTest(template = "simple")
  void shouldSupportNamedSnapshots(Snapshot snapshot) {
    snapshot.setVariable("title", "First State").assertMatchesSnapshot("state-1");
    snapshot.setVariable("title", "Second State").assertMatchesSnapshot("state-2");

    assertThat(snapshotPath("shouldSupportNamedSnapshots[state-1]"))
        .content()
        .contains("<h1>First State</h1>");
    assertThat(snapshotPath("shouldSupportNamedSnapshots[state-2]"))
        .content()
        .contains("<h1>Second State</h1>");
  }

  @SnapshotTest(inlineTemplate = "<div><span th:text=\"${value}\">val</span></div>")
  void shouldSupportFluentApi(Snapshot snapshot) {
    snapshot.setVariable("value", "fluent").assertMatchesSnapshot();

    assertThat(snapshotPath("shouldSupportFluentApi"))
        .content()
        .isEqualTo("<div><span>fluent</span></div>");
  }

  private static Path snapshotPath(String name) {
    return baseDir
        .resolve(ResolvedConfig.DEFAULT_SNAPSHOT_DIR)
        .resolve(ThymeleafSnapshotExtensionTest.class.getName())
        .resolve(name + ".html");
  }
}
