package com.github.suzumiyaaoba.thymeleaf.snapshot;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Thrown when the rendered template output does not match the stored snapshot.
 *
 * <p>This error extends {@link AssertionError} so that it is recognized by JUnit
 * as a test assertion failure. The error message includes a unified diff between
 * the expected (stored) and actual (rendered) content for easy debugging.</p>
 */
public final class SnapshotMismatchException extends AssertionError {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;

    private static final int CONTEXT_LINES = 3;

    private final String expected;
    private final String actual;
    private final Path snapshotPath;

    /**
     * Creates a new snapshot mismatch exception.
     *
     * @param snapshotPath the path to the snapshot file
     * @param expected     the expected content (from stored snapshot)
     * @param actual       the actual content (from rendering)
     * @throws NullPointerException if any argument is null
     */
    public SnapshotMismatchException(Path snapshotPath, String expected, String actual) {
        super(buildMessage(
                Objects.requireNonNull(snapshotPath, "snapshotPath must not be null"),
                Objects.requireNonNull(expected, "expected must not be null"),
                Objects.requireNonNull(actual, "actual must not be null")
        ));
        this.expected = expected;
        this.actual = actual;
        this.snapshotPath = snapshotPath;
    }

    /**
     * Returns the expected content from the stored snapshot.
     *
     * @return the expected content
     */
    public String getExpected() {
        return expected;
    }

    /**
     * Returns the actual rendered content.
     *
     * @return the actual content
     */
    public String getActual() {
        return actual;
    }

    /**
     * Returns the path to the snapshot file.
     *
     * @return the snapshot file path
     */
    public Path getSnapshotPath() {
        return snapshotPath;
    }

    private static String buildMessage(Path snapshotPath, String expected, String actual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Snapshot mismatch for: ").append(snapshotPath).append("\n\n");
        sb.append(generateUnifiedDiff(expected, actual));
        sb.append("\n\nTo update the snapshot, run with -D")
          .append(ThymeleafSnapshotExtension.UPDATE_PROPERTY).append("=true");
        sb.append(" or set @SnapshotTest(update = true)");
        return sb.toString();
    }

    /**
     * Generates a unified diff between two strings with context lines.
     *
     * <p>The output follows the standard unified diff format with
     * {@code @@ -L,C +L,C @@} hunk headers. Lines prefixed with {@code -} are
     * from the expected content, {@code +} from the actual content, and a space
     * for unchanged context lines.</p>
     *
     * @param expected the expected content
     * @param actual   the actual content
     * @return a unified diff string
     */
    static String generateUnifiedDiff(String expected, String actual) {
        List<String> expectedLines = Arrays.asList(expected.split("\n", -1));
        List<String> actualLines = Arrays.asList(actual.split("\n", -1));

        Patch<String> patch = DiffUtils.diff(expectedLines, actualLines);
        List<String> diff = UnifiedDiffUtils.generateUnifiedDiff(
                "expected (stored snapshot)",
                "actual (rendered output)",
                expectedLines,
                patch,
                CONTEXT_LINES
        );
        return String.join("\n", diff);
    }
}
