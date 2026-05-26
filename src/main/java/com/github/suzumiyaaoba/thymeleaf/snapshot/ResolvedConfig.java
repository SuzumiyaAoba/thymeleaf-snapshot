package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.util.Objects;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Resolved configuration values for a snapshot test, derived from the {@link SnapshotConfig}
 * annotation or defaults.
 *
 * <p>This record consolidates all configuration in a single immutable object, eliminating the need
 * to pass many individual parameters.
 */
record ResolvedConfig(
    String templatePrefix,
    String templateSuffix,
    String snapshotDir,
    boolean prettyPrint,
    String characterEncoding,
    TemplateMode templateMode) {

  /** Default template prefix. */
  static final String DEFAULT_TEMPLATE_PREFIX = "templates/";

  /** Default template suffix. */
  static final String DEFAULT_TEMPLATE_SUFFIX = ".html";

  /** Default snapshot directory name. */
  static final String DEFAULT_SNAPSHOT_DIR = "__snapshots__";

  /** Default character encoding. */
  static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

  ResolvedConfig {
    Objects.requireNonNull(templatePrefix, "templatePrefix must not be null");
    Objects.requireNonNull(templateSuffix, "templateSuffix must not be null");
    Objects.requireNonNull(snapshotDir, "snapshotDir must not be null");
    Objects.requireNonNull(characterEncoding, "characterEncoding must not be null");
    Objects.requireNonNull(templateMode, "templateMode must not be null");
  }

  /**
   * Returns the file extension for the snapshot file based on the template mode.
   *
   * @return file extension including the leading dot
   */
  String snapshotFileExtension() {
    return extensionForMode(templateMode);
  }

  /**
   * Maps a {@link TemplateMode} to its corresponding snapshot file extension.
   *
   * @param mode the template mode
   * @return file extension including the leading dot
   */
  static String extensionForMode(TemplateMode mode) {
    return switch (mode) {
      case HTML -> ".html";
      case XML -> ".xml";
      case TEXT -> ".txt";
      case JAVASCRIPT -> ".js";
      case CSS -> ".css";
      case RAW -> ".txt";
    };
  }

  /**
   * Creates a {@link ResolvedConfig} from a {@link SnapshotConfig} annotation. If the annotation is
   * {@code null}, default values are used.
   *
   * @param annotation the annotation, or null for defaults
   * @return the resolved configuration
   */
  static ResolvedConfig from(SnapshotConfig annotation) {
    if (annotation == null) {
      return defaults();
    }
    return new ResolvedConfig(
        annotation.templatePrefix(),
        annotation.templateSuffix(),
        annotation.snapshotDir(),
        annotation.prettyPrint(),
        annotation.characterEncoding(),
        annotation.templateMode());
  }

  /**
   * Returns a configuration with all default values.
   *
   * @return the default configuration
   */
  static ResolvedConfig defaults() {
    return new ResolvedConfig(
        DEFAULT_TEMPLATE_PREFIX,
        DEFAULT_TEMPLATE_SUFFIX,
        DEFAULT_SNAPSHOT_DIR,
        false,
        DEFAULT_CHARACTER_ENCODING,
        TemplateMode.HTML);
  }
}
