package com.github.suzumiyaaoba.thymeleaf.snapshot;

/**
 * Central definition of the system properties that control snapshot behaviour at runtime.
 *
 * <p>Keeping the property names and their parsing in one place avoids duplicating {@link
 * System#getProperty} calls across the codebase and gives a single source of truth for the
 * documented flags. The public constants exposed on {@link ThymeleafSnapshotExtension} delegate to
 * the names defined here.
 */
final class SnapshotProperties {

  /** Enables global snapshot update mode ({@code -Dsnapshot.update=true}). */
  static final String UPDATE = "snapshot.update";

  /** Overrides the snapshot base directory ({@code -Dsnapshot.baseDir=/path}). */
  static final String BASE_DIR = "snapshot.baseDir";

  /**
   * Enables CI mode, failing instead of auto-creating missing snapshots ({@code
   * -Dsnapshot.ci=true}).
   */
  static final String CI = "snapshot.ci";

  private SnapshotProperties() {}

  /**
   * Returns whether global snapshot update mode is enabled.
   *
   * @return {@code true} if {@code -Dsnapshot.update=true}
   */
  static boolean isUpdateEnabled() {
    return Boolean.getBoolean(UPDATE);
  }

  /**
   * Returns whether CI mode is enabled.
   *
   * @return {@code true} if {@code -Dsnapshot.ci=true}
   */
  static boolean isCiEnabled() {
    return Boolean.getBoolean(CI);
  }

  /**
   * Returns the configured base-directory override, or {@code null} when unset or blank.
   *
   * @return the override path, or {@code null}
   */
  static String baseDirOverride() {
    String override = System.getProperty(BASE_DIR);
    return (override == null || override.isBlank()) ? null : override;
  }
}
