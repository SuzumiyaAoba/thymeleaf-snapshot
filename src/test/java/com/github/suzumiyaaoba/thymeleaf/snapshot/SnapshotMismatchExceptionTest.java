package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotMismatchExceptionTest {

    @Test
    void shouldContainDiffInMessage() {
        String expected = "<html>\n<body>\n<p>Hello</p>\n</body>\n</html>";
        String actual = "<html>\n<body>\n<p>World</p>\n</body>\n</html>";
        Path path = Path.of("test.html");

        SnapshotMismatchException exception =
                new SnapshotMismatchException(path, expected, actual);

        assertThat(exception.getMessage())
                .isNotNull()
                .contains("Snapshot mismatch for:")
                .contains("test.html")
                .contains("-<p>Hello</p>")
                .contains("+<p>World</p>")
                .contains("-Dsnapshot.update=true");
    }

    @Test
    void shouldStoreExpectedAndActual() {
        String expected = "expected";
        String actual = "actual";
        Path path = Path.of("test.html");

        SnapshotMismatchException exception =
                new SnapshotMismatchException(path, expected, actual);

        assertThat(exception.getExpected()).isEqualTo(expected);
        assertThat(exception.getActual()).isEqualTo(actual);
        assertThat(exception.getSnapshotPath()).isEqualTo(path);
    }

    @Test
    void shouldBeAssertionError() {
        SnapshotMismatchException exception =
                new SnapshotMismatchException(Path.of("test.html"), "a", "b");

        assertThat(exception).isInstanceOf(AssertionError.class);
    }

    @Test
    void generateUnifiedDiffShowsContext() {
        String expected = "line1\nline2\nline3\nline4\nline5\nline6\nline7\nline8\nline9\nline10";
        String actual = "line1\nline2\nline3\nline4\nmodified5\nline6\nline7\nline8\nline9\nline10";

        String diff = SnapshotMismatchException.generateUnifiedDiff(expected, actual);

        assertThat(diff)
                .contains("--- expected (stored snapshot)")
                .contains("+++ actual (rendered output)")
                .contains("-line5")
                .contains("+modified5");
    }

    @Test
    void generateUnifiedDiffSingleInsertionLeavesUnchangedLinesAsContext() {
        String expected = "line1\nline2\nline3";
        String actual = "line1\nline2\ninserted\nline3";

        String diff = SnapshotMismatchException.generateUnifiedDiff(expected, actual);

        assertThat(diff)
                .as("inserted line should appear as addition")
                .contains("+inserted");
        assertThat(diff)
                .as("unchanged lines should not appear as removals")
                .doesNotContain("-line1", "-line2", "-line3");
    }

    @Test
    void generateUnifiedDiffSingleDeletionLeavesUnchangedLinesAsContext() {
        String expected = "line1\ndeleted\nline3";
        String actual = "line1\nline3";

        String diff = SnapshotMismatchException.generateUnifiedDiff(expected, actual);

        assertThat(diff)
                .as("deleted line should appear as removal")
                .contains("-deleted");
        assertThat(diff)
                .as("unchanged lines should not appear as additions")
                .doesNotContain("+line1", "+line3");
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
        assertThat(hunkCount)
                .as("Expected multiple hunks for distant changes, but got:\n" + diff)
                .isGreaterThanOrEqualTo(2);
    }
}
