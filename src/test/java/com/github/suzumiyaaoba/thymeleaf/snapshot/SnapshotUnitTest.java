package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotUnitTest {

    @TempDir
    Path tempDir;

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
            @Override public String template() { return template; }
            @Override public String inlineTemplate() { return inlineTemplate; }
            @Override public boolean update() { return update; }
            @Override public Class<? extends Annotation> annotationType() { return SnapshotTest.class; }
        };
    }

    private Snapshot inlineSnapshot(String tmpl, boolean prettyPrint, boolean globalUpdate) {
        return new Snapshot(renderer, manager, "TC", "tm",
                annotation("", tmpl, false), prettyPrint, globalUpdate);
    }

    // --- setVariables ---

    @Test
    void setVariables_addsAllEntries() throws Exception {
        var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, false);

        assertSame(s, s.setVariables(Map.of("msg", "hello")));

        s.assertMatchesSnapshot();
        String written = Files.readString(tempDir.resolve("TC/tm.html"));
        assertTrue(written.contains("hello"));
    }

    @Test
    void setVariables_throwsOnNull() {
        var s = inlineSnapshot("<p>x</p>", false, false);
        assertThrows(NullPointerException.class, () -> s.setVariables(null));
    }

    // --- setLocale ---

    @Test
    void setLocale_returnsThis() {
        var s = inlineSnapshot("<p>x</p>", false, false);
        assertSame(s, s.setLocale(Locale.ENGLISH));
    }

    @Test
    void setLocale_throwsOnNull() {
        var s = inlineSnapshot("<p>x</p>", false, false);
        assertThrows(NullPointerException.class, () -> s.setLocale(null));
    }

    // --- clearVariables ---

    @Test
    void clearVariables_removesAllVariables() throws Exception {
        var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, false);
        s.setVariable("msg", "before");

        assertSame(s, s.clearVariables());

        s.setVariable("msg", "after");
        s.assertMatchesSnapshot();
        String written = Files.readString(tempDir.resolve("TC/tm.html"));
        assertTrue(written.contains("after"));
        assertFalse(written.contains("before"));
    }

    // --- assertMatchesSnapshot: prettyPrint ---

    @Test
    void assertMatchesSnapshot_prettyPrintsWhenEnabled() throws Exception {
        var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", true, false);
        s.setVariable("msg", "pretty");

        s.assertMatchesSnapshot();

        String written = Files.readString(tempDir.resolve("TC/tm.html"));
        assertTrue(written.contains("\n"), "prettyPrint should add newlines");
    }

    // --- assertMatchesSnapshot: update mode ---

    @Test
    void assertMatchesSnapshot_updatesExistingSnapshotWhenGlobalUpdateTrue() throws Exception {
        Path snapshotPath = tempDir.resolve("TC/tm.html");
        Files.createDirectories(snapshotPath.getParent());
        Files.writeString(snapshotPath, "old content");

        var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, true);
        s.setVariable("msg", "new");
        s.assertMatchesSnapshot(); // should overwrite, not throw

        String written = Files.readString(snapshotPath);
        assertTrue(written.contains("new"));
        assertFalse(written.contains("old content"));
    }

    @Test
    void assertMatchesSnapshot_updatesExistingSnapshotWhenAnnotationUpdateTrue() throws Exception {
        Path snapshotPath = tempDir.resolve("TC/tm.html");
        Files.createDirectories(snapshotPath.getParent());
        Files.writeString(snapshotPath, "old content");

        var s = new Snapshot(renderer, manager, "TC", "tm",
                annotation("", "<p th:text=\"${msg}\">x</p>", true), false, false);
        s.setVariable("msg", "new");
        s.assertMatchesSnapshot();

        assertTrue(Files.readString(snapshotPath).contains("new"));
    }

    // --- assertMatchesSnapshot: mismatch ---

    @Test
    void assertMatchesSnapshot_throwsMismatchWhenDiffers() throws Exception {
        Path snapshotPath = tempDir.resolve("TC/tm.html");
        Files.createDirectories(snapshotPath.getParent());
        Files.writeString(snapshotPath, "<p>expected</p>");

        var s = inlineSnapshot("<p th:text=\"${msg}\">x</p>", false, false);
        s.setVariable("msg", "different");

        assertThrows(SnapshotMismatchException.class, () -> s.assertMatchesSnapshot());
    }

    // --- validateAnnotation ---

    @Test
    void validateAnnotation_throwsWhenBothTemplateAndInlineSpecified() {
        assertThrows(IllegalStateException.class, () ->
                new Snapshot(renderer, manager, "TC", "tm",
                        annotation("tmpl", "inline", false), false, false));
    }

    @Test
    void validateAnnotation_throwsWhenNeitherTemplateNorInlineSpecified() {
        assertThrows(IllegalStateException.class, () ->
                new Snapshot(renderer, manager, "TC", "tm",
                        annotation("", "", false), false, false));
    }
}
