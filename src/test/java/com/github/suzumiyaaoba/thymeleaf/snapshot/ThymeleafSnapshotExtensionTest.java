package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for the Thymeleaf Snapshot Testing extension.
 *
 * <p>These tests verify the complete workflow: template rendering, snapshot creation, comparison,
 * and update.
 */
@ExtendWith(ThymeleafSnapshotExtension.class)
class ThymeleafSnapshotExtensionTest {

  @AfterEach
  void cleanUpSnapshots() throws IOException {
    // Clean up generated snapshot files after tests
    Path snapshotDir =
        Path.of(
            "src",
            "test",
            "resources",
            "__snapshots__",
            ThymeleafSnapshotExtensionTest.class.getName());
    if (Files.exists(snapshotDir)) {
      Files.walkFileTree(
          snapshotDir,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }

  @Test
  void nonSnapshotTestMethodIsIgnoredByExtension() {
    // beforeEach's snapshotTest == null branch is exercised for plain @Test methods
  }

  @Test
  void resolveSnapshotMethodNameKeepsPlainTestMethodName() {
    String methodName =
        ThymeleafSnapshotExtension.resolveSnapshotMethodName(
            "shouldRender", "shouldRender()", "[engine:junit-jupiter]/[class:Example]");

    assertEquals("shouldRender", methodName);
  }

  @Test
  void resolveSnapshotMethodNameIncludesParameterizedInvocationDisplayName() {
    String methodName =
        ThymeleafSnapshotExtension.resolveSnapshotMethodName(
            "shouldRender",
            "[2] locale=en_US",
            "[engine:junit-jupiter]/[test-template:shouldRender]/[test-template-invocation:#2]");

    assertEquals("shouldRender[[2] locale=en_US]", methodName);
  }

  @Test
  void resolveSnapshotMethodNameSanitizesDisplayNameForFileSystemUse() {
    String methodName =
        ThymeleafSnapshotExtension.resolveSnapshotMethodName(
            "shouldRender",
            "[1] locale=en/US:variant?",
            "[engine:junit-jupiter]/[test-template:shouldRender]/[test-template-invocation:#1]");

    assertEquals("shouldRender[[1] locale=en_US_variant_]", methodName);
  }

  @SnapshotTest(template = "simple")
  void shouldCreateSnapshotOnFirstRun(Snapshot snapshot) {
    snapshot.setVariable("title", "Hello World");
    // First run: should create the snapshot and pass
    snapshot.assertMatchesSnapshot();
  }

  @SnapshotTest(inlineTemplate = "<p th:text=\"${message}\">placeholder</p>")
  void shouldRenderInlineTemplate(Snapshot snapshot) {
    snapshot.setVariable("message", "Inline Test");
    snapshot.assertMatchesSnapshot();
  }

  @SnapshotTest(template = "variables")
  void shouldRenderTemplateWithMultipleVariables(Snapshot snapshot) {
    snapshot.setVariable("pageTitle", "Test Page");
    snapshot.setVariable("heading", "Welcome");
    snapshot.setVariable("message", "Hello from test");
    snapshot.setVariable("items", List.of("Item 1", "Item 2", "Item 3"));
    snapshot.assertMatchesSnapshot();
  }

  @SnapshotTest(template = "simple")
  void shouldSupportNamedSnapshots(Snapshot snapshot) {
    snapshot.setVariable("title", "First State");
    snapshot.assertMatchesSnapshot("state-1");

    snapshot.setVariable("title", "Second State");
    snapshot.assertMatchesSnapshot("state-2");
  }

  @SnapshotTest(inlineTemplate = "<div><span th:text=\"${value}\">val</span></div>")
  void shouldSupportFluentApi(Snapshot snapshot) {
    snapshot.setVariable("value", "fluent").assertMatchesSnapshot();
  }
}
