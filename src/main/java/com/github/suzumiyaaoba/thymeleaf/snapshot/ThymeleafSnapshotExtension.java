package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that integrates Thymeleaf snapshot testing.
 *
 * <p>This extension handles the lifecycle of snapshot tests:
 *
 * <ul>
 *   <li>Initializes the Thymeleaf template engine based on {@link SnapshotConfig}
 *   <li>Creates and injects {@link Snapshot} instances into test methods
 *   <li>Manages snapshot file storage via {@link SnapshotManager}
 * </ul>
 *
 * <p>The {@link ThymeleafRenderer} and {@link SnapshotManager} are cached at the class level to
 * avoid re-creating heavyweight objects for each test method.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @ExtendWith(ThymeleafSnapshotExtension.class)
 * class MyTemplateTest {
 *
 *     @SnapshotTest(template = "pages/home")
 *     void shouldRenderHomePage(Snapshot snapshot) {
 *         snapshot.setVariable("title", "Hello");
 *         snapshot.assertMatchesSnapshot();
 *     }
 * }
 * }</pre>
 */
public class ThymeleafSnapshotExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(ThymeleafSnapshotExtension.class);

  private static final String SNAPSHOT_KEY = "snapshot";
  private static final String RENDERER_KEY = "renderer";
  private static final String MANAGER_KEY = "snapshotManager";

  /**
   * System property name to enable global snapshot update mode. Set {@code -Dsnapshot.update=true}
   * to update all snapshots.
   */
  public static final String UPDATE_PROPERTY = "snapshot.update";

  /**
   * System property name to override the snapshot base directory. Set {@code
   * -Dsnapshot.baseDir=/path/to/dir} to store snapshots under a specific directory instead of the
   * auto-detected project root.
   *
   * <p>Example: {@code ./gradlew test -Dsnapshot.baseDir=/tmp/snap}
   */
  public static final String BASE_DIR_PROPERTY = "snapshot.baseDir";

  @Override
  public void beforeEach(ExtensionContext context) {
    Method testMethod = context.getRequiredTestMethod();
    Class<?> testClass = context.getRequiredTestClass();

    SnapshotTest snapshotTest = testMethod.getAnnotation(SnapshotTest.class);
    if (snapshotTest == null) {
      return;
    }

    // Resolve configuration (cached at class level via annotation lookup)
    ResolvedConfig config = ResolvedConfig.from(testClass.getAnnotation(SnapshotConfig.class));

    // Get or create class-level components
    ThymeleafRenderer renderer = getOrCreateRenderer(context, config);
    SnapshotManager snapshotManager = getOrCreateSnapshotManager(context, config);

    // Check for global update mode
    boolean globalUpdate = Boolean.getBoolean(UPDATE_PROPERTY);

    // Create per-test Snapshot instance
    Snapshot snapshot =
        new Snapshot(
            renderer,
            snapshotManager,
            testClass.getName(),
            resolveSnapshotMethodName(
                testMethod.getName(), context.getDisplayName(), context.getUniqueId()),
            snapshotTest,
            config.prettyPrint(),
            globalUpdate);

    // Store in method-level extension context
    context.getStore(NAMESPACE).put(SNAPSHOT_KEY, snapshot);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(Snapshot.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Snapshot snapshot = extensionContext.getStore(NAMESPACE).get(SNAPSHOT_KEY, Snapshot.class);
    if (snapshot == null) {
      throw new ParameterResolutionException(
          "Snapshot parameter is only available in methods annotated with @SnapshotTest. "
              + "Make sure the test method has the @SnapshotTest annotation.");
    }
    return snapshot;
  }

  /** Gets the {@link ThymeleafRenderer} from the class-level store, creating it on first access. */
  private ThymeleafRenderer getOrCreateRenderer(ExtensionContext context, ResolvedConfig config) {
    ExtensionContext.Store classStore = getClassStore(context);
    return classStore.getOrComputeIfAbsent(
        RENDERER_KEY,
        key ->
            new ThymeleafRenderer(
                config.templatePrefix(), config.templateSuffix(), config.characterEncoding()),
        ThymeleafRenderer.class);
  }

  /** Gets the {@link SnapshotManager} from the class-level store, creating it on first access. */
  private SnapshotManager getOrCreateSnapshotManager(
      ExtensionContext context, ResolvedConfig config) {
    ExtensionContext.Store classStore = getClassStore(context);
    return classStore.getOrComputeIfAbsent(
        MANAGER_KEY, key -> new SnapshotManager(config.snapshotDir()), SnapshotManager.class);
  }

  /** Returns the class-level store for caching shared objects. */
  private ExtensionContext.Store getClassStore(ExtensionContext context) {
    return context.getParent().orElse(context).getStore(NAMESPACE);
  }

  static String resolveSnapshotMethodName(
      String methodName, String displayName, String uniqueId) {
    if (!isTestTemplateInvocation(uniqueId)) {
      return methodName;
    }
    return methodName + "[" + sanitizeSnapshotName(displayName) + "]";
  }

  private static boolean isTestTemplateInvocation(String uniqueId) {
    return uniqueId != null && uniqueId.contains("test-template-invocation");
  }

  private static String sanitizeSnapshotName(String value) {
    if (value == null || value.isBlank()) {
      return "invocation";
    }
    String sanitized = value.replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_").trim();
    return sanitized.isEmpty() ? "invocation" : sanitized;
  }
}
