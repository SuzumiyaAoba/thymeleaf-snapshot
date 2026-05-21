package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The main user-facing API for Thymeleaf snapshot testing.
 *
 * <p>An instance of this class is injected into test methods annotated with {@link SnapshotTest}.
 * It provides a fluent interface for setting template variables and asserting that the rendered
 * output matches a stored snapshot.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @SnapshotTest(template = "pages/home")
 * void shouldRenderHomePage(Snapshot snapshot) {
 *     snapshot.setVariable("title", "Hello World");
 *     snapshot.setVariable("items", List.of("A", "B"));
 *     snapshot.assertMatchesSnapshot();
 * }
 * }</pre>
 *
 * <h2>Multiple Snapshots per Test</h2>
 *
 * <pre>{@code
 * @SnapshotTest(template = "pages/dashboard")
 * void shouldRenderMultipleStates(Snapshot snapshot) {
 *     snapshot.setVariable("loggedIn", true);
 *     snapshot.assertMatchesSnapshot("logged-in");
 *
 *     snapshot.clearVariables();
 *     snapshot.setVariable("loggedIn", false);
 *     snapshot.assertMatchesSnapshot("logged-out");
 * }
 * }</pre>
 */
public final class Snapshot {

  private final ThymeleafRenderer renderer;
  private final SnapshotManager snapshotManager;
  private final String testClassName;
  private final String testMethodName;
  private final SnapshotTest annotation;
  private final boolean prettyPrint;
  private final boolean shouldUpdate;

  private final Map<String, Object> variables = new LinkedHashMap<>();
  private Locale locale = Locale.ROOT;

  /**
   * Creates a new Snapshot instance. This constructor is used internally by {@link
   * ThymeleafSnapshotExtension}.
   *
   * @param renderer the Thymeleaf renderer
   * @param snapshotManager the snapshot file manager
   * @param testClassName the fully qualified test class name
   * @param testMethodName the test method name
   * @param annotation the SnapshotTest annotation
   * @param prettyPrint whether to pretty-print HTML
   * @param globalUpdate whether global snapshot update is enabled
   */
  Snapshot(
      ThymeleafRenderer renderer,
      SnapshotManager snapshotManager,
      String testClassName,
      String testMethodName,
      SnapshotTest annotation,
      boolean prettyPrint,
      boolean globalUpdate) {
    this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
    this.snapshotManager =
        Objects.requireNonNull(snapshotManager, "snapshotManager must not be null");
    this.testClassName = Objects.requireNonNull(testClassName, "testClassName must not be null");
    this.testMethodName = Objects.requireNonNull(testMethodName, "testMethodName must not be null");
    this.annotation = Objects.requireNonNull(annotation, "annotation must not be null");
    this.prettyPrint = prettyPrint;
    this.shouldUpdate = globalUpdate || annotation.update();

    validateAnnotation();
  }

  /**
   * Sets a template variable.
   *
   * @param name the variable name
   * @param value the variable value (may be {@code null})
   * @return this snapshot instance for chaining
   * @throws NullPointerException if name is null
   */
  public Snapshot setVariable(String name, Object value) {
    Objects.requireNonNull(name, "variable name must not be null");
    variables.put(name, value);
    return this;
  }

  /**
   * Sets multiple template variables at once.
   *
   * @param variables a map of variable names to values
   * @return this snapshot instance for chaining
   * @throws NullPointerException if variables is null
   */
  public Snapshot setVariables(Map<String, Object> variables) {
    Objects.requireNonNull(variables, "variables must not be null");
    this.variables.putAll(variables);
    return this;
  }

  /**
   * Sets the locale for template rendering.
   *
   * <p>Defaults to {@link Locale#ROOT} for environment-independent snapshots. Call this method when
   * locale-sensitive formatting (e.g. date, number) is intentionally under test.
   *
   * @param locale the locale
   * @return this snapshot instance for chaining
   * @throws NullPointerException if locale is null
   */
  public Snapshot setLocale(Locale locale) {
    this.locale = Objects.requireNonNull(locale, "locale must not be null");
    return this;
  }

  /**
   * Clears all previously set variables.
   *
   * <p>Useful when asserting multiple snapshots within a single test where each snapshot should
   * start with a clean variable set.
   *
   * @return this snapshot instance for chaining
   */
  public Snapshot clearVariables() {
    variables.clear();
    return this;
  }

  /**
   * Asserts that the rendered template matches the stored snapshot.
   *
   * <p>If no snapshot exists, one is created automatically and the test passes. If a snapshot
   * exists, the rendered output is compared against it. If they don't match, a {@link
   * SnapshotMismatchException} is thrown with a detailed diff.
   *
   * @throws SnapshotMismatchException if the snapshot does not match
   */
  public void assertMatchesSnapshot() {
    assertMatchesSnapshot(null);
  }

  /**
   * Asserts that the rendered template matches a named snapshot.
   *
   * <p>Use this when you need multiple snapshots within a single test method. Each snapshot name
   * must be unique within the test method.
   *
   * @param snapshotName the name for this snapshot (may be {@code null} for default name)
   * @throws SnapshotMismatchException if the snapshot does not match
   */
  public void assertMatchesSnapshot(String snapshotName) {
    String rendered = renderTemplate();

    if (prettyPrint) {
      rendered = HtmlFormatter.prettyPrint(rendered);
    }

    Path snapshotPath =
        snapshotManager.resolveSnapshotPath(testClassName, testMethodName, snapshotName);

    if (!snapshotManager.snapshotExists(snapshotPath) || shouldUpdate) {
      snapshotManager.writeSnapshot(snapshotPath, rendered);
      return;
    }

    String expected = snapshotManager.readSnapshot(snapshotPath);
    if (!snapshotManager.matches(expected, rendered)) {
      throw new SnapshotMismatchException(snapshotPath, expected, rendered);
    }
  }

  private String renderTemplate() {
    String template = annotation.template();
    String inlineTemplate = annotation.inlineTemplate();

    if (!template.isEmpty()) {
      return renderer.render(template, variables, locale);
    } else {
      return renderer.renderInline(inlineTemplate, variables, locale);
    }
  }

  private void validateAnnotation() {
    boolean hasTemplate = !annotation.template().isEmpty();
    boolean hasInlineTemplate = !annotation.inlineTemplate().isEmpty();

    if (hasTemplate && hasInlineTemplate) {
      throw new IllegalStateException(
          "@SnapshotTest specifies both 'template' and 'inlineTemplate'. "
              + "These attributes are mutually exclusive — specify only one.");
    }
    if (!hasTemplate && !hasInlineTemplate) {
      throw new IllegalStateException(
          "@SnapshotTest requires either 'template' or 'inlineTemplate' to be specified.");
    }
  }
}
