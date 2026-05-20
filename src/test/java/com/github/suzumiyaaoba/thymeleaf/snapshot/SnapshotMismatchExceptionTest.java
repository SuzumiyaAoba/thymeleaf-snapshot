package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotMismatchExceptionTest {

    @Test
    void shouldContainDiffInMessage() {
        String expected = "<html>\n<body>\n<p>Hello</p>\n</body>\n</html>";
        String actual = "<html>\n<body>\n<p>World</p>\n</body>\n</html>";
        Path path = Path.of("test.html");

        SnapshotMismatchException exception =
                new SnapshotMismatchException(path, expected, actual);

        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Snapshot mismatch for:"));
        assertTrue(message.contains("test.html"));
        assertTrue(message.contains("-<p>Hello</p>"));
        assertTrue(message.contains("+<p>World</p>"));
        assertTrue(message.contains("-Dsnapshot.update=true"));
    }

    @Test
    void shouldStoreExpectedAndActual() {
        String expected = "expected";
        String actual = "actual";
        Path path = Path.of("test.html");

        SnapshotMismatchException exception =
                new SnapshotMismatchException(path, expected, actual);

        assertEquals(expected, exception.getExpected());
        assertEquals(actual, exception.getActual());
        assertEquals(path, exception.getSnapshotPath());
    }

    @Test
    void shouldBeAssertionError() {
        SnapshotMismatchException exception =
                new SnapshotMismatchException(Path.of("test.html"), "a", "b");

        assertInstanceOf(AssertionError.class, exception);
    }

    @Test
    void generateUnifiedDiffShowsContext() {
        String expected = "line1\nline2\nline3\nline4\nline5\nline6\nline7\nline8\nline9\nline10";
        String actual = "line1\nline2\nline3\nline4\nmodified5\nline6\nline7\nline8\nline9\nline10";

        String diff = SnapshotMismatchException.generateUnifiedDiff(expected, actual);

        assertTrue(diff.contains("--- expected (stored snapshot)"));
        assertTrue(diff.contains("+++ actual (rendered output)"));
        assertTrue(diff.contains("-line5"));
        assertTrue(diff.contains("+modified5"));
    }

    @Test
    void generateUnifiedDiffSingleInsertionLeavesUnchangedLinesAsContext() {
        String expected = "line1\nline2\nline3";
        String actual = "line1\nline2\ninserted\nline3";

        String diff = SnapshotMismatchException.generateUnifiedDiff(expected, actual);

        assertTrue(diff.contains("+inserted"), "inserted line should appear as addition");
        assertFalse(diff.contains("-line1"), "unchanged line1 should not appear as removal");
        assertFalse(diff.contains("-line2"), "unchanged line2 should not appear as removal");
        assertFalse(diff.contains("-line3"), "unchanged line3 should not appear as removal");
    }

    @Test
    void generateUnifiedDiffSingleDeletionLeavesUnchangedLinesAsContext() {
        String expected = "line1\ndeleted\nline3";
        String actual = "line1\nline3";

        String diff = SnapshotMismatchException.generateUnifiedDiff(expected, actual);

        assertTrue(diff.contains("-deleted"), "deleted line should appear as removal");
        assertFalse(diff.contains("+line1"), "unchanged line1 should not appear as addition");
        assertFalse(diff.contains("+line3"), "unchanged line3 should not appear as addition");
    }

    @Test
    void generateUnifiedDiffSeparatesDistantChangesIntoMultipleHunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            if (i > 1) sb.append("\n");
            sb.append("line").append(i);
        }
        String expected = sb.toString();
        String actual = expected.replace("line1\n", "changed1\n").replace("\nline20", "\nchanged20");

        String diff = SnapshotMismatchException.generateUnifiedDiff(expected, actual);

        long hunkCount = diff.lines().filter(l -> l.startsWith("@@")).count();
        assertTrue(hunkCount >= 2, "Expected multiple hunks for distant changes, but got:\n" + diff);
    }
}
