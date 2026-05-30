package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

  @Test
  void resolveSnapshotPathWithoutName() {
    Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", null);

    assertThat(path).isEqualTo(tempDir.resolve("com.example.MyTest").resolve("testMethod.html"));
  }

  @Test
  void resolveSnapshotPathWithName() {
    Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", "mobile");

    assertThat(path)
        .isEqualTo(tempDir.resolve("com.example.MyTest").resolve("testMethod[mobile].html"));
  }

  @Test
  void resolveSnapshotPathWithEmptyName() {
    Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", "");

    assertThat(path).isEqualTo(tempDir.resolve("com.example.MyTest").resolve("testMethod.html"));
  }

  @Test
  void resolveSnapshotPathSanitizesIllegalChars() {
    record Case(String input, String expectedFile) {}
    var cases =
        new Case[] {
          new Case("mobile/landscape", "testMethod[mobile_landscape].html"),
          new Case("a\\b", "testMethod[a_b].html"),
          new Case("a:b", "testMethod[a_b].html"),
          new Case("snap*shot", "testMethod[snap_shot].html"),
          new Case("snap?shot", "testMethod[snap_shot].html"),
          new Case("snap\"shot", "testMethod[snap_shot].html"),
          new Case("snap<shot", "testMethod[snap_shot].html"),
          new Case("snap>shot", "testMethod[snap_shot].html"),
          new Case("snap|shot", "testMethod[snap_shot].html"),
          new Case("   ", "testMethod[snapshot].html"),
        };
    for (var c : cases) {
      Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", c.input());
      assertThat(path)
          .as("snapshotName='%s'", c.input())
          .isEqualTo(tempDir.resolve("com.example.MyTest").resolve(c.expectedFile()));
    }
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

  @Test
  void matchesReturnsTrueForEqualContent() {
    assertThat(manager.matches("hello", "hello")).isTrue();
  }

  @Test
  void matchesReturnsFalseForDifferentContent() {
    assertThat(manager.matches("hello", "world")).isFalse();
  }

  @Test
  void matchesIgnoresTrailingNewlineOnExpected() {
    assertThat(manager.matches("hello\n", "hello")).isTrue();
  }

  @Test
  void matchesIgnoresTrailingNewlineOnActual() {
    assertThat(manager.matches("hello", "hello\n")).isTrue();
  }

  @Test
  void matchesIgnoresTrailingCrLfOnBothSides() {
    assertThat(manager.matches("hello\r\n", "hello\r\n")).isTrue();
  }

  @Test
  void matchesTreatsCrLfAndLfAsEqual() {
    assertThat(manager.matches("line1\r\nline2", "line1\nline2")).isTrue();
  }

  @Test
  void matchesTreatsStoredCrLfAsEqualToRenderedLf() {
    assertThat(manager.matches("<p>a</p>\r\n<p>b</p>", "<p>a</p>\n<p>b</p>")).isTrue();
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
  void writeSnapshotStripsTrailingNewline() {
    Path path = manager.resolveSnapshotPath("com.example.Test", "trailingNewline", null);

    manager.writeSnapshot(path, "<html/>\n");

    assertThat(manager.readSnapshot(path)).isEqualTo("<html/>");
  }

  @Test
  void readSnapshotStripsTrailingNewlineAddedByEditor() throws IOException {
    Path path = manager.resolveSnapshotPath("com.example.Test", "editorNewline", null);
    Files.createDirectories(path.getParent());
    Files.writeString(path, "<html/>\n", StandardCharsets.UTF_8);

    assertThat(manager.readSnapshot(path)).isEqualTo("<html/>");
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
