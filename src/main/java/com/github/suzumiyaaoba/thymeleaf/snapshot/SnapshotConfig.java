package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Class-level annotation to configure the Thymeleaf snapshot test environment.
 *
 * <p>Apply this annotation to a test class to customize the Thymeleaf template engine settings and
 * snapshot storage options.
 *
 * <pre>{@code
 * @ExtendWith(ThymeleafSnapshotExtension.class)
 * @SnapshotConfig(templatePrefix = "templates/", prettyPrint = true)
 * class MyTemplateTest {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SnapshotConfig {

  /**
   * Prefix for resolving template names. Templates are loaded from the classpath relative to this
   * prefix.
   *
   * @return the template prefix
   */
  String templatePrefix() default "templates/";

  /**
   * Suffix appended to template names during resolution.
   *
   * @return the template suffix
   */
  String templateSuffix() default ".html";

  /**
   * Directory name for storing snapshot files, relative to {@code src/test/resources/}.
   *
   * @return the snapshot directory name
   */
  String snapshotDir() default "__snapshots__";

  /**
   * Whether to pretty-print the rendered HTML before storing as a snapshot. When enabled, the HTML
   * is formatted using Jsoup for better readability and more meaningful diffs.
   *
   * @return true if pretty-printing is enabled
   */
  boolean prettyPrint() default false;

  /**
   * Character encoding for template processing.
   *
   * @return the character encoding
   */
  String characterEncoding() default "UTF-8";

  /**
   * Thymeleaf template mode. Determines the rendering engine behaviour and the file extension used
   * for snapshot files ({@code .html}, {@code .xml}, {@code .txt}, {@code .js}, {@code .css}).
   * {@code prettyPrint} is silently ignored for non-HTML modes.
   *
   * @return the template mode
   */
  TemplateMode templateMode() default TemplateMode.HTML;
}
