package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Regression tests for issue #40: orphaned-snapshot handling must be skipped when not every {@link
 * SnapshotTest} method of the class completed successfully in the run, so that a {@code --tests}
 * filter, {@code @Disabled}, or an aborted assumption combined with {@code -Dsnapshot.update=true}
 * cannot delete snapshots of tests that simply did not run.
 *
 * <p>Fixture classes are executed in-process through the JUnit Platform {@link Launcher} and are
 * guarded by a system property so the surrounding Gradle run never executes them directly.
 */
class OrphanCleanupGuardTest {

  private static final String FIXTURE_FLAG = "thymeleaf.snapshot.test.fixture";

  @TempDir Path baseDir;

  private String previousBaseDir;
  private String previousUpdate;
  private String previousFlag;

  @BeforeEach
  void enableFixtureEnvironment() {
    previousBaseDir = System.setProperty(SnapshotProperties.BASE_DIR, baseDir.toString());
    previousUpdate = System.setProperty(SnapshotProperties.UPDATE, "true");
    previousFlag = System.setProperty(FIXTURE_FLAG, "true");
  }

  @AfterEach
  void restoreSystemProperties() {
    restore(SnapshotProperties.BASE_DIR, previousBaseDir);
    restore(SnapshotProperties.UPDATE, previousUpdate);
    restore(FIXTURE_FLAG, previousFlag);
  }

  @Test
  void filteredRunDoesNotDeleteSnapshotsOfTestsThatDidNotRun() throws IOException {
    Path classDir = fixtureClassDir(TwoSnapshotTestsFixture.class);
    Path otherSnapshot = Files.writeString(classDir.resolve("two.html"), "<p>two</p>");
    Path staleSnapshot = Files.writeString(classDir.resolve("obsolete.html"), "<p>gone</p>");

    TestExecutionSummary summary =
        launch(selectMethod(TwoSnapshotTestsFixture.class, "one", Snapshot.class));

    assertThat(summary.getTestsSucceededCount()).isEqualTo(1);
    assertThat(summary.getTotalFailureCount()).isZero();
    // 'two' did not run, so neither its snapshot nor anything else may be deleted
    assertThat(otherSnapshot).exists();
    assertThat(staleSnapshot).exists();
  }

  @Test
  void fullRunStillDeletesOrphanedSnapshots() throws IOException {
    Path classDir = fixtureClassDir(TwoSnapshotTestsFixture.class);
    Path staleSnapshot = Files.writeString(classDir.resolve("obsolete.html"), "<p>gone</p>");

    TestExecutionSummary summary = launch(selectClass(TwoSnapshotTestsFixture.class));

    assertThat(summary.getTestsSucceededCount()).isEqualTo(2);
    assertThat(summary.getTotalFailureCount()).isZero();
    assertThat(staleSnapshot).doesNotExist();
    assertThat(classDir.resolve("one.html")).exists();
    assertThat(classDir.resolve("two.html")).exists();
  }

  @Test
  void abortedTestPreventsOrphanCleanup() throws IOException {
    Path classDir = fixtureClassDir(AbortingFixture.class);
    Path abortedSnapshot = Files.writeString(classDir.resolve("aborted.html"), "<p>kept</p>");
    Path staleSnapshot = Files.writeString(classDir.resolve("obsolete.html"), "<p>gone</p>");

    TestExecutionSummary summary = launch(selectClass(AbortingFixture.class));

    assertThat(summary.getTestsAbortedCount()).isEqualTo(1);
    assertThat(summary.getTestsSucceededCount()).isEqualTo(1);
    // the aborted test never reached its assertion, so cleanup must not run at all
    assertThat(abortedSnapshot).exists();
    assertThat(staleSnapshot).exists();
  }

  @Test
  void fullRunIncludesInheritedSnapshotMethods() throws IOException {
    Path classDir = fixtureClassDir(InheritedFixture.class);
    Path staleSnapshot = Files.writeString(classDir.resolve("obsolete.html"), "<p>gone</p>");

    TestExecutionSummary summary = launch(selectClass(InheritedFixture.class));

    assertThat(summary.getTestsSucceededCount()).isEqualTo(2);
    assertThat(summary.getTotalFailureCount()).isZero();
    assertThat(staleSnapshot).doesNotExist();
    assertThat(classDir.resolve("inherited.html")).content().isEqualTo("<p>base</p>");
    assertThat(classDir.resolve("child.html")).content().isEqualTo("<p>child</p>");
  }

  @Test
  void filteredRunKeepsSnapshotsOfInheritedTests() throws IOException {
    Path classDir = fixtureClassDir(InheritedFixture.class);
    Path inheritedSnapshot = Files.writeString(classDir.resolve("inherited.html"), "<p>base</p>");
    Path staleSnapshot = Files.writeString(classDir.resolve("obsolete.html"), "<p>gone</p>");

    TestExecutionSummary summary =
        launch(selectMethod(InheritedFixture.class, "child", Snapshot.class));

    assertThat(summary.getTestsSucceededCount()).isEqualTo(1);
    assertThat(summary.getTotalFailureCount()).isZero();
    assertThat(inheritedSnapshot).content().isEqualTo("<p>base</p>");
    assertThat(staleSnapshot).exists();
  }

  @Test
  void nestedRunDoesNotCountEnclosingMethodsWithTheSameName() throws IOException {
    Path outerDir = fixtureClassDir(NestedFixture.class);
    Path outerOrphan = Files.writeString(outerDir.resolve("obsolete.html"), "<p>gone</p>");
    Path nestedDir = fixtureClassDir(NestedFixture.PartialNestedFixture.class);
    Path skippedSnapshot = Files.writeString(nestedDir.resolve("render.html"), "<p>kept</p>");
    Path nestedOrphan = Files.writeString(nestedDir.resolve("obsolete.html"), "<p>gone</p>");

    TestExecutionSummary summary = launch(selectClass(NestedFixture.class));

    assertThat(summary.getTestsSucceededCount()).isEqualTo(2);
    assertThat(summary.getTestsSkippedCount()).isEqualTo(1);
    assertThat(summary.getTotalFailureCount()).isZero();
    assertThat(outerOrphan).doesNotExist();
    assertThat(skippedSnapshot).content().isEqualTo("<p>kept</p>");
    assertThat(nestedOrphan).exists();
    assertThat(nestedDir.resolve("active.html")).content().isEqualTo("<p>active</p>");
  }

  private Path fixtureClassDir(Class<?> fixtureClass) throws IOException {
    return Files.createDirectories(
        baseDir.resolve(ResolvedConfig.DEFAULT_SNAPSHOT_DIR).resolve(fixtureClass.getName()));
  }

  private static TestExecutionSummary launch(DiscoverySelector selector) {
    Launcher launcher = LauncherFactory.create();
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.execute(
        LauncherDiscoveryRequestBuilder.request().selectors(selector).build(), listener);
    return listener.getSummary();
  }

  private static void restore(String key, String previousValue) {
    if (previousValue == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, previousValue);
    }
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class TwoSnapshotTestsFixture {

    @SnapshotTest(inlineTemplate = "<p>one</p>")
    void one(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }

    @SnapshotTest(inlineTemplate = "<p>two</p>")
    void two(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class AbortingFixture {

    @SnapshotTest(inlineTemplate = "<p>ok</p>")
    void ok(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }

    @SnapshotTest(inlineTemplate = "<p>aborted</p>")
    void aborted(Snapshot snapshot) {
      Assumptions.assumeTrue(false, "intentionally aborted");
    }
  }

  abstract static class BaseFixture {
    @SnapshotTest(inlineTemplate = "<p>base</p>")
    void inherited(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class InheritedFixture extends BaseFixture {
    @SnapshotTest(inlineTemplate = "<p>child</p>")
    void child(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class NestedFixture {
    @SnapshotTest(inlineTemplate = "<p>outer</p>")
    void render(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }

    @Nested
    class PartialNestedFixture {
      @SnapshotTest(inlineTemplate = "<p>active</p>")
      void active(Snapshot snapshot) {
        snapshot.assertMatchesSnapshot();
      }

      @Disabled("intentionally skipped")
      @SnapshotTest(inlineTemplate = "<p>kept</p>")
      void render(Snapshot snapshot) {
        snapshot.assertMatchesSnapshot();
      }
    }
  }
}
