package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Manages snapshot file storage, retrieval, and comparison.
 *
 * <p>Snapshot files are stored under the configured snapshot directory (default: {@code
 * src/test/resources/__snapshots__/}), organized by test class and method name.
 *
 * <p>File naming convention:
 *
 * <pre>
 * __snapshots__/
 *   com.example.MyTest/
 *     testMethodName.html
 *     testMethodName[snapshot-name].html
 * </pre>
 *
 * <h2>Line-ending and trailing-newline policy</h2>
 *
 * <p>CRLF ({@code \r\n}) sequences are normalized to LF ({@code \n}) on both read and write so that
 * snapshot files checked out with {@code core.autocrlf=true} on Windows do not cause spurious
 * mismatches. Trailing newlines are also stripped before comparison and before writing, so editors
 * configured to append a final newline (VS Code, Vim {@code fixendofline}, Prettier, etc.) do not
 * cause spurious mismatches.
 */
public final class SnapshotManager {

  private static final String ILLEGAL_FILENAME_CHARS = "[<>:\"/\\\\|?*\\p{Cntrl}]";

  private final Path snapshotBaseDir;

  /**
   * Creates a new snapshot manager that auto-detects the project root and stores snapshots under
   * {@code src/test/resources/<snapshotDirName>/}.
   *
   * @param snapshotDirName the snapshot directory name (e.g., {@code "__snapshots__"})
   * @throws NullPointerException if snapshotDirName is null
   */
  public SnapshotManager(String snapshotDirName) {
    Objects.requireNonNull(snapshotDirName, "snapshotDirName must not be null");
    this.snapshotBaseDir = resolveSnapshotBaseDir(snapshotDirName);
  }

  /**
   * Creates a new snapshot manager with an explicit base directory.
   *
   * <p>This constructor is primarily intended for testing, allowing the snapshot directory to be
   * injected (e.g., using {@code @TempDir}).
   *
   * @param snapshotBaseDir the base directory for snapshot storage
   * @throws NullPointerException if snapshotBaseDir is null
   */
  SnapshotManager(Path snapshotBaseDir) {
    this.snapshotBaseDir =
        Objects.requireNonNull(snapshotBaseDir, "snapshotBaseDir must not be null");
  }

  /**
   * Resolves the snapshot file path for a given test.
   *
   * @param testClassName the fully qualified test class name
   * @param testMethodName the test method name
   * @param snapshotName optional snapshot name (for multiple snapshots per test), may be {@code
   *     null}
   * @return the path to the snapshot file
   * @throws NullPointerException if testClassName or testMethodName is null
   */
  public Path resolveSnapshotPath(
      String testClassName, String testMethodName, String snapshotName) {
    Objects.requireNonNull(testClassName, "testClassName must not be null");
    Objects.requireNonNull(testMethodName, "testMethodName must not be null");

    String fileName;
    if (snapshotName != null && !snapshotName.isEmpty()) {
      fileName = testMethodName + "[" + sanitizeSnapshotName(snapshotName) + "].html";
    } else {
      fileName = testMethodName + ".html";
    }
    return snapshotBaseDir.resolve(testClassName).resolve(fileName);
  }

  /**
   * Checks whether a snapshot file exists.
   *
   * @param snapshotPath the path to the snapshot file
   * @return {@code true} if the snapshot file exists
   * @throws NullPointerException if snapshotPath is null
   */
  public boolean snapshotExists(Path snapshotPath) {
    Objects.requireNonNull(snapshotPath, "snapshotPath must not be null");
    return Files.exists(snapshotPath);
  }

  /**
   * Reads the content of a stored snapshot file, normalizing line endings and stripping trailing
   * newlines.
   *
   * @param snapshotPath the path to the snapshot file
   * @return the snapshot content with CRLF normalized to LF and trailing newlines removed
   * @throws NullPointerException if snapshotPath is null
   * @throws UncheckedIOException if the file cannot be read
   */
  public String readSnapshot(Path snapshotPath) {
    Objects.requireNonNull(snapshotPath, "snapshotPath must not be null");
    try {
      return normalize(Files.readString(snapshotPath, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read snapshot: " + snapshotPath, e);
    }
  }

  /**
   * Writes content to a snapshot file, creating parent directories if necessary.
   *
   * <p>CRLF sequences are normalized to LF and trailing newlines are stripped before writing to
   * ensure files are stored in a canonical form regardless of OS or editor settings.
   *
   * @param snapshotPath the path to the snapshot file
   * @param content the content to write
   * @throws NullPointerException if snapshotPath or content is null
   * @throws UncheckedIOException if the file cannot be written
   */
  public void writeSnapshot(Path snapshotPath, String content) {
    Objects.requireNonNull(snapshotPath, "snapshotPath must not be null");
    Objects.requireNonNull(content, "content must not be null");
    try {
      Files.createDirectories(snapshotPath.getParent());
      Files.writeString(snapshotPath, normalize(content), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write snapshot: " + snapshotPath, e);
    }
  }

  /**
   * Compares two content strings for equality after normalizing line endings and trailing newlines
   * on both sides.
   *
   * @param expected the expected content
   * @param actual the actual content
   * @return {@code true} if the contents are equal after normalization
   */
  public boolean matches(String expected, String actual) {
    return Objects.equals(normalize(expected), normalize(actual));
  }

  /**
   * Returns snapshot files under the given test class directory that were not accessed during the
   * current test run.
   *
   * <p>A file is considered orphaned when it exists on disk but its path was never recorded by
   * {@link #resolveSnapshotPath} during this run — typically because the test method was renamed or
   * deleted.
   *
   * @param testClassName the fully qualified test class name
   * @param accessedPaths the set of snapshot paths that were accessed in this run
   * @return a sorted list of orphaned snapshot file paths; empty if the class directory does not
   *     exist or all files were accessed
   */
  List<Path> findOrphanedSnapshots(String testClassName, Set<Path> accessedPaths) {
    Path classDir = snapshotBaseDir.resolve(testClassName);
    if (!Files.exists(classDir)) {
      return Collections.emptyList();
    }
    List<Path> orphans = new ArrayList<>();
    try (var stream = Files.walk(classDir)) {
      stream
          .filter(Files::isRegularFile)
          .filter(p -> !accessedPaths.contains(p))
          .forEach(orphans::add);
    } catch (IOException e) {
      return Collections.emptyList();
    }
    Collections.sort(orphans);
    return Collections.unmodifiableList(orphans);
  }

  /**
   * Returns the base directory for snapshot storage.
   *
   * @return the snapshot base directory path
   */
  public Path getSnapshotBaseDir() {
    return snapshotBaseDir;
  }

  static String normalize(String s) {
    return stripTrailingNewlines(s.replace("\r\n", "\n"));
  }

  private static String stripTrailingNewlines(String s) {
    int end = s.length();
    while (end > 0 && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')) {
      end--;
    }
    return end == s.length() ? s : s.substring(0, end);
  }

  private static String sanitizeSnapshotName(String snapshotName) {
    String sanitized = snapshotName.replaceAll(ILLEGAL_FILENAME_CHARS, "_").trim();
    return sanitized.isEmpty() ? "snapshot" : sanitized;
  }

  private static Path resolveSnapshotBaseDir(String snapshotDirName) {
    // Allow explicit override via system property
    String override = System.getProperty(ThymeleafSnapshotExtension.BASE_DIR_PROPERTY);
    if (override != null && !override.isBlank()) {
      return Paths.get(override).resolve(snapshotDirName);
    }

    // Try to find the project root by looking for test resources on the classpath
    // and navigating up to the project root
    URL testResourcesUrl = Thread.currentThread().getContextClassLoader().getResource("");
    if (testResourcesUrl != null) {
      try {
        Path classesDir = Paths.get(testResourcesUrl.toURI());
        // Navigate from build/classes/java/test or build/resources/test to project root
        Path projectRoot = findProjectRoot(classesDir);
        return projectRoot
            .resolve("src")
            .resolve("test")
            .resolve("resources")
            .resolve(snapshotDirName);
      } catch (URISyntaxException
          | FileSystemNotFoundException
          | IllegalArgumentException
          | UnsupportedOperationException e) {
        // Fall through to default
      }
    }

    // Fallback: use current working directory
    return Paths.get("src", "test", "resources", snapshotDirName);
  }

  private static Path findProjectRoot(Path startPath) {
    Path current = startPath;
    while (current != null) {
      if (Files.exists(current.resolve("build.gradle"))
          || Files.exists(current.resolve("build.gradle.kts"))
          || Files.exists(current.resolve("pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    // Fallback: assume we're 4 levels deep (build/classes/java/test)
    Path root = startPath;
    for (int i = 0; i < 4 && root.getParent() != null; i++) {
      root = root.getParent();
    }
    return root;
  }
}
