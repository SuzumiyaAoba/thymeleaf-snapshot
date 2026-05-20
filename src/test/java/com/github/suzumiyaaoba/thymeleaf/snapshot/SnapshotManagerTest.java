package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

        assertEquals(tempDir.resolve("com.example.MyTest").resolve("testMethod.html"), path);
    }

    @Test
    void resolveSnapshotPathWithName() {
        Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", "mobile");

        assertEquals(tempDir.resolve("com.example.MyTest").resolve("testMethod[mobile].html"), path);
    }

    @Test
    void resolveSnapshotPathWithEmptyName() {
        Path path = manager.resolveSnapshotPath("com.example.MyTest", "testMethod", "");

        assertEquals(tempDir.resolve("com.example.MyTest").resolve("testMethod.html"), path);
    }

    @Test
    void snapshotExistsReturnsFalseForMissing() {
        Path path = tempDir.resolve("nonexistent.html");

        assertFalse(manager.snapshotExists(path));
    }

    @Test
    void writeAndReadSnapshot() {
        Path path = manager.resolveSnapshotPath("com.example.Test", "myTest", null);
        String content = "<html><body>Hello</body></html>";

        manager.writeSnapshot(path, content);

        assertTrue(manager.snapshotExists(path));
        assertEquals(content, manager.readSnapshot(path));
    }

    @Test
    void writeSnapshotCreatesDirectories() {
        Path path = manager.resolveSnapshotPath("com.example.deep.Test", "myTest", null);
        String content = "test content";

        manager.writeSnapshot(path, content);

        assertTrue(Files.exists(path));
        assertEquals(content, manager.readSnapshot(path));
    }

    @Test
    void matchesReturnsTrueForEqualContent() {
        assertTrue(manager.matches("hello", "hello"));
    }

    @Test
    void matchesReturnsFalseForDifferentContent() {
        assertFalse(manager.matches("hello", "world"));
    }

    @Test
    void writeSnapshotOverwritesExisting() throws IOException {
        Path path = manager.resolveSnapshotPath("com.example.Test", "myTest", null);

        manager.writeSnapshot(path, "first version");
        manager.writeSnapshot(path, "second version");

        assertEquals("second version", manager.readSnapshot(path));
    }

    @Test
    void resolveSnapshotBaseDirRespectsSystemProperty() {
        System.setProperty(ThymeleafSnapshotExtension.BASE_DIR_PROPERTY, tempDir.toString());

        SnapshotManager propertyManager = new SnapshotManager("__snapshots__");

        assertEquals(
                tempDir.resolve("__snapshots__"),
                propertyManager.getSnapshotBaseDir()
        );
    }
}
