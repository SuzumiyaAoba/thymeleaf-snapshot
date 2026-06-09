package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Regression test for issue #39: a {@code @Nested} class that declares its own {@link
 * SnapshotConfig} must get a renderer and snapshot manager built from that config, not the
 * enclosing class's cached instances found via the extension store's ancestor lookup.
 */
@ExtendWith(ThymeleafSnapshotExtension.class)
@SnapshotConfig(templateMode = TemplateMode.HTML)
class NestedSnapshotConfigOverrideTest {

  private static final String NESTED_SNAPSHOT_DIR = "__nested_snapshots__";

  @AfterAll
  static void cleanUpSnapshots() throws IOException {
    deleteRecursively(
        Path.of(
            "src",
            "test",
            "resources",
            "__snapshots__",
            NestedSnapshotConfigOverrideTest.class.getName()));
    deleteRecursively(Path.of("src", "test", "resources", NESTED_SNAPSHOT_DIR));
  }

  // Runs before the nested class and populates the class-level store with HTML-mode
  // components; the nested tests must not pick those up.
  @SnapshotTest(inlineTemplate = "<p th:text=\"${m}\">x</p>")
  void outerUsesHtmlMode(Snapshot snapshot) {
    snapshot.setVariable("m", "outer").assertMatchesSnapshot();
  }

  @Nested
  @SnapshotConfig(templateMode = TemplateMode.TEXT, snapshotDir = NESTED_SNAPSHOT_DIR)
  class WithOverriddenConfig {

    @SnapshotTest(inlineTemplate = "<p th:text=\"${m}\">x</p>")
    void nestedUsesItsOwnConfig(Snapshot snapshot) throws IOException {
      snapshot.setVariable("m", "inner").assertMatchesSnapshot();

      Path expected =
          Path.of(
              "src",
              "test",
              "resources",
              NESTED_SNAPSHOT_DIR,
              WithOverriddenConfig.class.getName(),
              "nestedUsesItsOwnConfig.txt");
      assertTrue(
          Files.exists(expected),
          "snapshot must be stored under the nested class's snapshotDir: " + expected);
      // TEXT mode leaves markup untouched; the HTML-mode renderer would produce <p>inner</p>
      assertEquals("<p th:text=\"${m}\">x</p>", Files.readString(expected, StandardCharsets.UTF_8));
    }
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    Files.walkFileTree(
        dir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
            Files.delete(d);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
