package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SnapshotManagerTest {

  @TempDir Path tempDir;

  private SnapshotManager manager;

  @BeforeEach
  void setUp() {
    manager = new SnapshotManager(tempDir);
  }

  @AfterEach
  void clearSystemProperty() {
    System.clearProperty(ThymeleafSnapshotExtension.BASE_DIR_PROPERTY);
  }

  static Stream<Arguments> snapshotPathCases() {
    return Stream.of(
        arguments(null, "testMethod.html"),
        arguments("", "testMethod.html"),
        arguments("mobile", "testMethod[mobile].html"),
        arguments("mobile/landscape", "testMethod[mobile_landscape].html"),
        arguments("a\\b", "testMethod[a_b].html"),
        arguments("a:b", "testMethod[a_b].html"),
        arguments("snap*shot", "testMethod[snap_shot].html"),
        arguments("snap?shot", "testMethod[snap_shot].html"),
        arguments("snap\"shot", "testMethod[snap_shot].html"),
        arguments("snap<shot", "testMethod[snap_shot].html"),
        arguments("snap>shot", "testMethod[snap_shot].html"),
        arguments("snap|shot", "testMethod[snap_shot].html"),
        arguments("   ", "testMethod[snapshot].html"));
  }

  @ParameterizedTest(name = "snapshotName=[{0}] -> {1}")
  @MethodSource("snapshotPathCases")
  void resolveSnapshotPathBuildsAndSanitizesFileName(String snapshotName, String expectedFile) {
    Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", snapshotName);

    assertThat(path)
        .as("snapshotName='%s'", snapshotName)
        .isEqualTo(tempDir.resolve("com.example.MyTest").resolve(expectedFile));
  }

  @Test
  void snapshotExistsReturnsFalseForMissing() {
    Path path = tempDir.resolve("nonexistent.html");

    assertThat(manager.snapshotExists(path)).isFalse();
  }

  @Test
  void writeAndReadSnapshot() {
    Path path = manager.resolveSnapshotPath("com.example.Test", "myTest", null);
    String content = "<html><body>Hello</body></html>";

    manager.writeSnapshot(path, content);

    assertThat(manager.snapshotExists(path)).isTrue();
    assertThat(manager.readSnapshot(path)).isEqualTo(content);
  }

  @Test
  void writeSnapshotCreatesDirectories() {
    Path path = manager.resolveSnapshotPath("com.example.deep.Test", "myTest", null);
    String content = "test content";

    manager.writeSnapshot(path, content);

    assertThat(path).exists();
    assertThat(manager.readSnapshot(path)).isEqualTo(content);
  }

  static Stream<Arguments> matchesCases() {
    return Stream.of(
        arguments("equal content matches", "hello", "hello", true),
        arguments("different content does not match", "hello", "world", false),
        arguments("trailing newline on expected is ignored", "hello\n", "hello", true),
        arguments("trailing newline on actual is ignored", "hello", "hello\n", true),
        arguments("trailing CRLF on both sides is ignored", "hello\r\n", "hello\r\n", true),
        arguments("CRLF and LF are treated as equal", "line1\r\nline2", "line1\nline2", true),
        arguments(
            "stored CRLF equals rendered LF", "<p>a</p>\r\n<p>b</p>", "<p>a</p>\n<p>b</p>", true));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("matchesCases")
  void matchesNormalizesLineEndingsAndTrailingNewlines(
      String description, String expected, String actual, boolean shouldMatch) {
    assertThat(manager.matches(expected, actual)).as(description).isEqualTo(shouldMatch);
  }

  @Test
  void writeSnapshotNormalizesCrlfToLf() throws IOException {
    Path path = manager.resolveSnapshotPath("com.example.Test", "crlfWrite", null);

    manager.writeSnapshot(path, "line1\r\nline2\r\nline3");

    String stored = Files.readString(path, StandardCharsets.UTF_8);
    assertThat(stored).doesNotContain("\r\n");
    assertThat(manager.readSnapshot(path)).isEqualTo("line1\nline2\nline3");
  }

  @Test
  void readSnapshotNormalizesCrlfToLf() throws IOException {
    Path path = manager.resolveSnapshotPath("com.example.Test", "crlfRead", null);
    Files.createDirectories(path.getParent());
    Files.writeString(path, "line1\r\nline2\r\nline3", StandardCharsets.UTF_8);

    assertThat(manager.readSnapshot(path)).isEqualTo("line1\nline2\nline3");
  }

  @Test
  void writeSnapshotPreservesTrailingNewline() {
    Path path = manager.resolveSnapshotPath("com.example.Test", "trailingNewline", null);

    manager.writeSnapshot(path, "<html/>\n");

    assertThat(manager.readSnapshot(path)).isEqualTo("<html/>\n");
  }

  @Test
  void readSnapshotPreservesTrailingNewlineAddedByEditor() throws IOException {
    Path path = manager.resolveSnapshotPath("com.example.Test", "editorNewline", null);
    Files.createDirectories(path.getParent());
    Files.writeString(path, "<html/>\n", StandardCharsets.UTF_8);

    assertThat(manager.readSnapshot(path)).isEqualTo("<html/>\n");
  }

  @Test
  void matchesIgnoresEditorAddedTrailingNewlineInStoredContent() throws IOException {
    Path path = manager.resolveSnapshotPath("com.example.Test", "editorMatch", null);
    Files.createDirectories(path.getParent());
    Files.writeString(path, "<html/>\n", StandardCharsets.UTF_8);

    assertThat(manager.matches(manager.readSnapshot(path), "<html/>")).isTrue();
  }

  @Test
  void resolveSnapshotPathRejectsExtensionWithPathSeparator() {
    assertThatThrownBy(() -> manager.resolveSnapshotPath("TC", "tm", null, "/../evil.txt"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fileExtension");
  }

  @Test
  void resolveSnapshotPathRejectsExtensionWithoutLeadingDot() {
    assertThatThrownBy(() -> manager.resolveSnapshotPath("TC", "tm", null, "html"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resolveSnapshotPathAcceptsStandardExtensions() {
    for (String ext : new String[] {".html", ".xml", ".txt", ".js", ".css"}) {
      assertThat(manager.resolveSnapshotPath("TC", "m", null, ext).toString())
          .as("should accept extension %s", ext)
          .endsWith("m" + ext);
    }
  }

  @Test
  void writeSnapshotOverwritesExisting() throws IOException {
    Path path = manager.resolveSnapshotPath("com.example.Test", "myTest", null);

    manager.writeSnapshot(path, "first version");
    manager.writeSnapshot(path, "second version");

    assertThat(manager.readSnapshot(path)).isEqualTo("second version");
  }

  @Test
  void readSnapshot_throwsUncheckedIOExceptionWhenFileMissing() {
    Path missing = tempDir.resolve("nonexistent").resolve("file.html");

    assertThatThrownBy(() -> manager.readSnapshot(missing))
        .isInstanceOf(UncheckedIOException.class);
  }

  @Test
  void writeSnapshot_throwsUncheckedIOExceptionOnIOError() throws IOException {
    // Place a regular file where writeSnapshot expects a directory
    Path blockingFile = tempDir.resolve("com.example.Blocked");
    Files.createFile(blockingFile);
    Path snapshot = blockingFile.resolve("testMethod.html");

    assertThatThrownBy(() -> manager.writeSnapshot(snapshot, "content"))
        .isInstanceOf(UncheckedIOException.class);
  }

  @Test
  void resolveSnapshotBaseDirRespectsSystemProperty() {
    System.setProperty(ThymeleafSnapshotExtension.BASE_DIR_PROPERTY, tempDir.toString());

    SnapshotManager propertyManager = new SnapshotManager("__snapshots__");

    assertThat(propertyManager.getSnapshotBaseDir()).isEqualTo(tempDir.resolve("__snapshots__"));
  }

  // --- findOrphanedSnapshots ---

  @Test
  void findOrphanedSnapshots_returnsFilesNotInAccessedSet() throws Exception {
    Path a = manager.resolveSnapshotPath("MyTest", "methodA", null);
    Path b = manager.resolveSnapshotPath("MyTest", "methodB", null);
    manager.writeSnapshot(a, "a");
    manager.writeSnapshot(b, "b");

    List<Path> orphans = manager.findOrphanedSnapshots("MyTest", Set.of(a));

    assertThat(orphans).containsExactly(b);
  }

  @Test
  void findOrphanedSnapshots_returnsEmptyWhenClassDirectoryAbsent() {
    List<Path> orphans = manager.findOrphanedSnapshots("NonExistentTest", Set.of());

    assertThat(orphans).isEmpty();
  }

  @Test
  void findOrphanedSnapshots_returnsEmptyWhenAllSnapshotsAccessed() throws Exception {
    Path a = manager.resolveSnapshotPath("MyTest", "methodA", null);
    manager.writeSnapshot(a, "a");

    List<Path> orphans = manager.findOrphanedSnapshots("MyTest", Set.of(a));

    assertThat(orphans).isEmpty();
  }

  @Test
  void findOrphanedSnapshots_returnsAllFilesWhenNoneAccessed() throws Exception {
    Path a = manager.resolveSnapshotPath("MyTest", "methodA", null);
    Path b = manager.resolveSnapshotPath("MyTest", "methodB", null);
    manager.writeSnapshot(a, "a");
    manager.writeSnapshot(b, "b");

    List<Path> orphans = manager.findOrphanedSnapshots("MyTest", Set.of());

    assertThat(orphans).containsExactlyInAnyOrder(a, b);
  }
}
