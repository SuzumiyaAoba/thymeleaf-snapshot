package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Thrown when a snapshot file does not exist and CI mode is active.
 *
 * <p>In CI mode ({@code -Dsnapshot.ci=true} or the {@code CI} environment variable set to {@code
 * true}), auto-creating a missing snapshot would silently make a test pass without ever asserting
 * anything. This exception prevents that by requiring all snapshot files to be committed before the
 * test can pass.
 *
 * <p>To resolve this error, run the test locally (without CI mode) to generate the snapshot file,
 * then commit it alongside the test.
 */
public final class SnapshotMissingException extends AssertionError {

  @SuppressWarnings("unused")
  private static final long serialVersionUID = 1L;

  private final Path snapshotPath;

  /**
   * Creates a new snapshot-missing exception.
   *
   * @param snapshotPath the path where the snapshot file was expected
   * @throws NullPointerException if snapshotPath is null
   */
  public SnapshotMissingException(Path snapshotPath) {
    super(buildMessage(Objects.requireNonNull(snapshotPath, "snapshotPath must not be null")));
    this.snapshotPath = snapshotPath;
  }

  /**
   * Returns the path where the snapshot file was expected.
   *
   * @return the expected snapshot file path
   */
  public Path getSnapshotPath() {
    return snapshotPath;
  }

  private static String buildMessage(Path snapshotPath) {
    return "Snapshot file does not exist: "
        + snapshotPath
        + "\n\nSnapshot auto-creation is disabled in CI mode."
        + "\nRun the test locally (without -D"
        + ThymeleafSnapshotExtension.CI_PROPERTY
        + "=true) to generate the snapshot,"
        + "\nthen commit the file alongside the test.";
  }
}
