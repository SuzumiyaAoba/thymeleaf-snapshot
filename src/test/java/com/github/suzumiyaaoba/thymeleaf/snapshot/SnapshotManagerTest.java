package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotManagerTest {

    @TempDir
    Path tempDir;

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

        assertThat(path).isEqualTo(tempDir.resolve("com.example.MyTest").resolve("testMethod[mobile].html"));
    }

    @Test
    void resolveSnapshotPathWithEmptyName() {
        Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", "");

        assertThat(path).isEqualTo(tempDir.resolve("com.example.MyTest").resolve("testMethod.html"));
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
}
