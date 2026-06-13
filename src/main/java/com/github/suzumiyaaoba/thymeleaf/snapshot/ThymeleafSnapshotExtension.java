package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * JUnit 5 extension that integrates Thymeleaf snapshot testing.
 *
 * <p>This extension handles the lifecycle of snapshot tests:
 *
 * <ul>
 *   <li>Initializes the Thymeleaf template engine based on {@link SnapshotConfig}
 *   <li>Creates and injects {@link Snapshot} instances into test methods
 *   <li>Manages snapshot file storage via {@link SnapshotManager}
 *   <li>Reports (or deletes under update mode) orphaned snapshot files after all tests complete —
 *       but only when every {@link SnapshotTest} method declared on the class completed
 *       successfully in this run. Partial runs (a {@code --tests} filter, {@code @Disabled}, an
 *       aborted assumption, or a failed test) skip orphan handling entirely so that snapshots of
 *       tests that simply did not run are never reported or deleted.
 * </ul>
 *
 * <p>The {@link ThymeleafRenderer} and {@link SnapshotManager} are cached at the class level to
 * avoid re-creating heavyweight objects for each test method. Cache entries are keyed by the
 * resolved configuration values, so a {@code @Nested} class that declares its own {@link
 * SnapshotConfig} gets components built from that config rather than reusing the enclosing class's.
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
    implements BeforeEachCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(ThymeleafSnapshotExtension.class);

  private static final String SNAPSHOT_KEY = "snapshot";
  private static final String ACCESSED_PATHS_KEY = "accessedPaths";
  private static final String COMPLETED_METHODS_KEY = "completedMethods";

  /**
   * Store key for the cached {@link ThymeleafRenderer}. The key carries every config value the
   * renderer is built from, so a {@code @Nested} class that overrides {@link SnapshotConfig} gets
   * its own renderer instead of inheriting the enclosing class's cached one via the store's
   * ancestor lookup, while classes sharing the same config still share one instance.
   */
  private record RendererKey(
      String templatePrefix,
      String templateSuffix,
      String characterEncoding,
      TemplateMode templateMode) {
    static RendererKey of(ResolvedConfig config) {
      return new RendererKey(
          config.templatePrefix(),
          config.templateSuffix(),
          config.characterEncoding(),
          config.templateMode());
    }
  }

  /**
   * Store key for the cached {@link SnapshotManager}; keyed by snapshot directory for the same
   * reason as {@link RendererKey}.
   */
  private record ManagerKey(String snapshotDir) {
    static ManagerKey of(ResolvedConfig config) {
      return new ManagerKey(config.snapshotDir());
    }
  }

  /**
   * System property name to enable global snapshot update mode. Set {@code -Dsnapshot.update=true}
   * to update all snapshots.
   */
  public static final String UPDATE_PROPERTY = SnapshotProperties.UPDATE;

  /**
   * System property name to override the snapshot base directory. Set {@code
   * -Dsnapshot.baseDir=/path/to/dir} to store snapshots under a specific directory instead of the
   * auto-detected project root.
   *
   * <p>Example: {@code ./gradlew test -Dsnapshot.baseDir=/tmp/snap}
   */
  public static final String BASE_DIR_PROPERTY = SnapshotProperties.BASE_DIR;

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
  public static final String CI_PROPERTY = SnapshotProperties.CI;

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
    boolean globalUpdate = SnapshotProperties.isUpdateEnabled();

    // Check for CI mode via explicit system property
    boolean ciMode = SnapshotProperties.isCiEnabled();

    // Shared set that tracks every snapshot path accessed in this test class run
    Set<Path> accessedPaths = getOrCreateAccessedPaths(context);

    // Create per-test Snapshot instance
    Snapshot snapshot =
        new Snapshot(
            renderer,
            snapshotManager,
            testClass.getName(),
            testMethod.getName(),
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
  public void afterEach(ExtensionContext context) {
    Method testMethod = context.getRequiredTestMethod();
    if (testMethod.getAnnotation(SnapshotTest.class) == null) {
      return;
    }
    // Record only tests that completed without failing or aborting; a test that did not reach
    // its assertMatchesSnapshot calls must not allow its snapshots to be treated as orphans.
    if (context.getExecutionException().isEmpty()) {
      getOrCreateCompletedMethods(context)
          .add(qualifiedMethodName(context.getRequiredTestClass(), testMethod.getName()));
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    ExtensionContext.Store classStore = context.getStore(NAMESPACE);
    ResolvedConfig config =
        ResolvedConfig.from(resolveSnapshotConfig(context.getRequiredTestClass()));
    SnapshotManager manager = classStore.get(ManagerKey.of(config), SnapshotManager.class);
    if (manager == null) {
      return;
    }

    @SuppressWarnings("unchecked")
    Set<Path> accessedPaths = (Set<Path>) classStore.get(ACCESSED_PATHS_KEY);
    if (accessedPaths == null) {
      return;
    }

    Class<?> testClass = context.getRequiredTestClass();
    String testClassName = testClass.getName();

    @SuppressWarnings("unchecked")
    Set<String> completedMethods = (Set<String>) classStore.get(COMPLETED_METHODS_KEY);
    if (completedMethods == null
        || !completedMethods.containsAll(declaredSnapshotTestMethods(testClass))) {
      // Partial run: a --tests filter, @Disabled, an aborted assumption, or a failure means
      // some snapshots were legitimately not accessed — they must not be treated as orphans.
      if (SnapshotProperties.isUpdateEnabled()) {
        System.err.println(
            "[thymeleaf-snapshot] Skipping orphaned-snapshot cleanup for "
                + testClassName
                + ": not every @SnapshotTest method completed successfully in this run.");
      }
      return;
    }

    List<Path> orphans = manager.findOrphanedSnapshots(testClassName, accessedPaths);
    if (orphans.isEmpty()) {
      return;
    }

    boolean globalUpdate = SnapshotProperties.isUpdateEnabled();
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
        RendererKey.of(config),
        key ->
            new ThymeleafRenderer(
                key.templatePrefix(),
                key.templateSuffix(),
                key.characterEncoding(),
                key.templateMode()),
        ThymeleafRenderer.class);
  }

  /** Gets the {@link SnapshotManager} from the class-level store, creating it on first access. */
  private SnapshotManager getOrCreateSnapshotManager(
      ExtensionContext context, ResolvedConfig config) {
    ExtensionContext.Store classStore = getClassStore(context);
    return classStore.getOrComputeIfAbsent(
        ManagerKey.of(config),
        key -> new SnapshotManager(key.snapshotDir()),
        SnapshotManager.class);
  }

  /** Gets (or lazily creates) the shared set of accessed snapshot paths for this test class. */
  @SuppressWarnings("unchecked")
  private Set<Path> getOrCreateAccessedPaths(ExtensionContext context) {
    return (Set<Path>)
        getClassStore(context)
            .getOrComputeIfAbsent(ACCESSED_PATHS_KEY, key -> ConcurrentHashMap.newKeySet());
  }

  /**
   * Gets (or lazily creates) the shared set of successfully completed {@link SnapshotTest} method
   * names (qualified as {@code <class>#<method>}) for this test class run.
   */
  @SuppressWarnings("unchecked")
  private Set<String> getOrCreateCompletedMethods(ExtensionContext context) {
    return (Set<String>)
        getClassStore(context)
            .getOrComputeIfAbsent(COMPLETED_METHODS_KEY, key -> ConcurrentHashMap.newKeySet());
  }

  /**
   * Returns the qualified names ({@code <class>#<method>}) of every {@link SnapshotTest} method a
   * full run of the given class would execute, including methods inherited from superclasses.
   * Methods of {@code @Nested} classes are not included; each nested class is checked by its own
   * {@code afterAll} invocation.
   */
  static Set<String> declaredSnapshotTestMethods(Class<?> testClass) {
    Set<String> qualifiedNames = new HashSet<>();
    for (Class<?> cls = testClass; cls != null && cls != Object.class; cls = cls.getSuperclass()) {
      for (Method method : cls.getDeclaredMethods()) {
        if (method.getAnnotation(SnapshotTest.class) != null) {
          qualifiedNames.add(qualifiedMethodName(testClass, method.getName()));
        }
      }
    }
    return qualifiedNames;
  }

  private static String qualifiedMethodName(Class<?> testClass, String methodName) {
    return testClass.getName() + "#" + methodName;
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
}
