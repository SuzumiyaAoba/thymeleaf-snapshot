package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.AfterAllCallback;
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
 *   <li>Reports (or deletes under update mode) orphaned snapshot files after all tests complete
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
 *         snapshot
 *             .setVariable("title", "Hello")
 *             .assertMatchesSnapshot();
 *     }
 * }
 * }</pre>
 */
public class ThymeleafSnapshotExtension
    implements BeforeEachCallback, AfterAllCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(ThymeleafSnapshotExtension.class);

  private static final String SNAPSHOT_KEY = "snapshot";
  private static final String RENDERER_KEY = "renderer";
  private static final String MANAGER_KEY = "snapshotManager";
  private static final String ACCESSED_PATHS_KEY = "accessedPaths";

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

  /**
   * System property name to enable CI mode. Set {@code -Dsnapshot.ci=true} to make the test fail
   * (instead of auto-creating the file) when a snapshot file does not exist.
   *
   * <p>This prevents a test from silently passing on CI when its snapshot file was not committed.
   * Add this flag to your CI build command to enforce that all snapshots are committed:
   *
   * <pre>{@code
   * ./gradlew test -Dsnapshot.ci=true
   * }</pre>
   */
  public static final String CI_PROPERTY = "snapshot.ci";

  @Override
  public void beforeEach(ExtensionContext context) {
    Method testMethod = context.getRequiredTestMethod();
    Class<?> testClass = context.getRequiredTestClass();

    SnapshotTest snapshotTest = testMethod.getAnnotation(SnapshotTest.class);
    if (snapshotTest == null) {
      return;
    }

    // Resolve configuration (cached at class level via annotation lookup)
    ResolvedConfig config = ResolvedConfig.from(resolveSnapshotConfig(testClass));

    // Get or create class-level components
    ThymeleafRenderer renderer = getOrCreateRenderer(context, config);
    SnapshotManager snapshotManager = getOrCreateSnapshotManager(context, config);

    // Check for global update mode
    boolean globalUpdate = Boolean.getBoolean(UPDATE_PROPERTY);

    // Check for CI mode via explicit system property
    boolean ciMode = Boolean.getBoolean(CI_PROPERTY);

    // Shared set that tracks every snapshot path accessed in this test class run
    Set<Path> accessedPaths = getOrCreateAccessedPaths(context);

    // Create per-test Snapshot instance
    Snapshot snapshot =
        new Snapshot(
            renderer,
            snapshotManager,
            testClass.getName(),
            resolveSnapshotMethodName(testMethod.getName()),
            snapshotTest,
            config.prettyPrint(),
            globalUpdate,
            ciMode,
            accessedPaths,
            config.templateMode());

    // Store in method-level extension context
    context.getStore(NAMESPACE).put(SNAPSHOT_KEY, snapshot);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    ExtensionContext.Store classStore = context.getStore(NAMESPACE);
    SnapshotManager manager = classStore.get(MANAGER_KEY, SnapshotManager.class);
    if (manager == null) {
      return;
    }

    @SuppressWarnings("unchecked")
    Set<Path> accessedPaths = (Set<Path>) classStore.get(ACCESSED_PATHS_KEY);
    if (accessedPaths == null) {
      return;
    }

    String testClassName = context.getRequiredTestClass().getName();
    List<Path> orphans = manager.findOrphanedSnapshots(testClassName, accessedPaths);
    if (orphans.isEmpty()) {
      return;
    }

    boolean globalUpdate = Boolean.getBoolean(UPDATE_PROPERTY);
    for (Path orphan : orphans) {
      if (globalUpdate) {
        try {
          Files.delete(orphan);
          System.err.println("[thymeleaf-snapshot] Deleted orphaned snapshot: " + orphan);
        } catch (IOException e) {
          System.err.println(
              "[thymeleaf-snapshot] Failed to delete orphaned snapshot: "
                  + orphan
                  + " ("
                  + e.getMessage()
                  + ")");
        }
      } else {
        System.err.println(
            "[thymeleaf-snapshot] Orphaned snapshot (not accessed in this test run): " + orphan);
      }
    }
    if (!globalUpdate) {
      System.err.println(
          "[thymeleaf-snapshot] Run with -D"
              + UPDATE_PROPERTY
              + "=true to delete orphaned snapshots automatically.");
    }
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
                config.templatePrefix(),
                config.templateSuffix(),
                config.characterEncoding(),
                config.templateMode()),
        ThymeleafRenderer.class);
  }

  /** Gets the {@link SnapshotManager} from the class-level store, creating it on first access. */
  private SnapshotManager getOrCreateSnapshotManager(
      ExtensionContext context, ResolvedConfig config) {
    ExtensionContext.Store classStore = getClassStore(context);
    return classStore.getOrComputeIfAbsent(
        MANAGER_KEY, key -> new SnapshotManager(config.snapshotDir()), SnapshotManager.class);
  }

  /** Gets (or lazily creates) the shared set of accessed snapshot paths for this test class. */
  @SuppressWarnings("unchecked")
  private Set<Path> getOrCreateAccessedPaths(ExtensionContext context) {
    return (Set<Path>)
        getClassStore(context)
            .getOrComputeIfAbsent(ACCESSED_PATHS_KEY, key -> ConcurrentHashMap.newKeySet());
  }

  /** Returns the class-level store for caching shared objects. */
  private ExtensionContext.Store getClassStore(ExtensionContext context) {
    return context.getParent().orElse(context).getStore(NAMESPACE);
  }

  static SnapshotConfig resolveSnapshotConfig(Class<?> testClass) {
    Class<?> cls = testClass;
    while (cls != null) {
      SnapshotConfig cfg = cls.getAnnotation(SnapshotConfig.class);
      if (cfg != null) return cfg;
      cls = cls.getEnclosingClass();
    }
    return null;
  }

  static String resolveSnapshotMethodName(String methodName) {
    return methodName;
  }
}
