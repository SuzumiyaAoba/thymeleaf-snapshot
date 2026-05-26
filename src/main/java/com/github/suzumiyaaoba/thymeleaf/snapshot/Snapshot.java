package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.nio.file.Path;
import java.util.Collections;
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
 * <h2>Immutability</h2>
 *
 * <p>{@code Snapshot} is <strong>immutable</strong>. Methods such as {@link #setVariable}, {@link
 * #setVariables}, {@link #setLocale}, and {@link #clearVariables} do <em>not</em> modify the
 * receiver; they each return a <em>new</em> {@code Snapshot} instance with the requested change
 * applied. The original instance is unchanged.
 *
 * <p>This means that {@link #assertMatchesSnapshot()} always operates on a fixed, fully-determined
 * set of variables and locale — there are no hidden mutations that could affect a subsequent call.
 * Always capture or immediately consume the return value of the fluent methods:
 *
 * <pre>{@code
 * // Correct — chain fluently
 * snapshot.setVariable("title", "Hello").assertMatchesSnapshot();
 *
 * // Incorrect — setVariable result is discarded; assertMatchesSnapshot sees no variables
 * snapshot.setVariable("title", "Hello");
 * snapshot.assertMatchesSnapshot();          // BUG: title is not set on this instance
 * }</pre>
 *
 * <h2>Concurrency contract</h2>
 *
 * <p>Because every {@code Snapshot} instance is fully immutable after construction, <em>read</em>
 * operations ({@link #assertMatchesSnapshot()}) are safe to call from multiple threads
 * concurrently. However, each test method receives its own injected instance and is expected to use
 * it from a single thread; sharing an instance across threads for coordinated rendering is an
 * unusual pattern and offers no advantage.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @SnapshotTest(template = "pages/home")
 * void shouldRenderHomePage(Snapshot snapshot) {
 *     snapshot
 *         .setVariable("title", "Hello World")
 *         .setVariable("items", List.of("A", "B"))
 *         .assertMatchesSnapshot();
 * }
 * }</pre>
 *
 * <h2>Multiple Snapshots per Test</h2>
 *
 * <pre>{@code
 * @SnapshotTest(template = "pages/dashboard")
 * void shouldRenderMultipleStates(Snapshot snapshot) {
 *     snapshot
 *         .setVariable("loggedIn", true)
 *         .assertMatchesSnapshot("logged-in");
 *
 *     snapshot
 *         .setVariable("loggedIn", false)
 *         .assertMatchesSnapshot("logged-out");
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
  private final boolean ciMode;

  private final Map<String, Object> variables;
  private final Locale locale;

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
   * @param ciMode whether CI mode is active (fail instead of auto-creating missing snapshots)
   */
  Snapshot(
      ThymeleafRenderer renderer,
      SnapshotManager snapshotManager,
      String testClassName,
      String testMethodName,
      SnapshotTest annotation,
      boolean prettyPrint,
      boolean globalUpdate,
      boolean ciMode) {
    this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
    this.snapshotManager =
        Objects.requireNonNull(snapshotManager, "snapshotManager must not be null");
    this.testClassName = Objects.requireNonNull(testClassName, "testClassName must not be null");
    this.testMethodName = Objects.requireNonNull(testMethodName, "testMethodName must not be null");
    this.annotation = Objects.requireNonNull(annotation, "annotation must not be null");
    this.prettyPrint = prettyPrint;
    this.shouldUpdate = globalUpdate || annotation.update();
    this.ciMode = ciMode;
    this.variables = Collections.emptyMap();
    this.locale = Locale.ROOT;

    validateAnnotation();
  }

  private Snapshot(Snapshot base, Map<String, Object> variables, Locale locale) {
    this.renderer = base.renderer;
    this.snapshotManager = base.snapshotManager;
    this.testClassName = base.testClassName;
    this.testMethodName = base.testMethodName;
    this.annotation = base.annotation;
    this.prettyPrint = base.prettyPrint;
    this.shouldUpdate = base.shouldUpdate;
    this.ciMode = base.ciMode;
    this.variables = Collections.unmodifiableMap(variables);
    this.locale = locale;
  }

  /**
   * Returns a new {@code Snapshot} with the given variable added.
   *
   * <p>The receiver is not modified.
   *
   * @param name the variable name
   * @param value the variable value (may be {@code null})
   * @return a new {@code Snapshot} instance containing the added variable
   * @throws NullPointerException if name is null
   */
  public Snapshot setVariable(String name, Object value) {
    Objects.requireNonNull(name, "variable name must not be null");
    Map<String, Object> newVars = new LinkedHashMap<>(variables);
    newVars.put(name, value);
    return new Snapshot(this, newVars, locale);
  }

  /**
   * Returns a new {@code Snapshot} with all entries from the given map added.
   *
   * <p>The receiver is not modified.
   *
   * @param variables a map of variable names to values
   * @return a new {@code Snapshot} instance containing the added variables
   * @throws NullPointerException if variables is null
   */
  public Snapshot setVariables(Map<String, Object> variables) {
    Objects.requireNonNull(variables, "variables must not be null");
    Map<String, Object> newVars = new LinkedHashMap<>(this.variables);
    newVars.putAll(variables);
    return new Snapshot(this, newVars, locale);
  }

  /**
   * Returns a new {@code Snapshot} with the given locale.
   *
   * <p>Defaults to {@link Locale#ROOT} for environment-independent snapshots. Call this method when
   * locale-sensitive formatting (e.g. date, number) is intentionally under test.
   *
   * <p>The receiver is not modified.
   *
   * @param locale the locale
   * @return a new {@code Snapshot} instance with the updated locale
   * @throws NullPointerException if locale is null
   */
  public Snapshot setLocale(Locale locale) {
    Objects.requireNonNull(locale, "locale must not be null");
    return new Snapshot(this, new LinkedHashMap<>(variables), locale);
  }

  /**
   * Returns a new {@code Snapshot} with all variables cleared.
   *
   * <p>Useful when asserting multiple snapshots within a single test where each snapshot should
   * start with a clean variable set.
   *
   * <p>The receiver is not modified.
   *
   * @return a new {@code Snapshot} instance with no variables
   */
  public Snapshot clearVariables() {
    return new Snapshot(this, new LinkedHashMap<>(), locale);
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

    boolean exists = snapshotManager.snapshotExists(snapshotPath);
    if (!exists) {
      if (ciMode && !shouldUpdate) {
        throw new SnapshotMissingException(snapshotPath);
      }
      snapshotManager.writeSnapshot(snapshotPath, rendered);
      return;
    }
    if (shouldUpdate) {
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

    if (!template.isBlank()) {
      return renderer.render(template, variables, locale);
    } else {
      return renderer.renderInline(inlineTemplate, variables, locale);
    }
  }

  private void validateAnnotation() {
    boolean hasTemplate = !annotation.template().isBlank();
    boolean hasInlineTemplate = !annotation.inlineTemplate().isBlank();

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
