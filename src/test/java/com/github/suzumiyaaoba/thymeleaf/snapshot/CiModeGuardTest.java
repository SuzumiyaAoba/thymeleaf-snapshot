package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/** Regression tests for issue #41: CI mode must never create, overwrite, or delete snapshots. */
class CiModeGuardTest {

  private static final String FIXTURE_FLAG = "thymeleaf.snapshot.test.ciFixture";

  @TempDir Path baseDir;

  private Properties savedProperties;

  @BeforeEach
  void enableCiEnvironment() {
    savedProperties = (Properties) System.getProperties().clone();
    System.setProperty(SnapshotProperties.BASE_DIR, baseDir.toString());
    System.setProperty(SnapshotProperties.CI, "true");
    System.clearProperty(SnapshotProperties.UPDATE);
    System.setProperty(FIXTURE_FLAG, "true");
  }

  @AfterEach
  void restoreSystemProperties() {
    System.setProperties(savedProperties);
  }

  @ParameterizedTest
  @CsvSource({"false, false", "false, true", "true, false", "true, true"})
  void rejectsUpdateWithoutModifyingSnapshotFiles(boolean annotationUpdate, boolean exists)
      throws IOException {
    Class<?> fixture = annotationUpdate ? AnnotationUpdateFixture.class : GlobalUpdateFixture.class;
    System.setProperty(SnapshotProperties.UPDATE, Boolean.toString(!annotationUpdate));
    Path classDir = fixtureClassDir(fixture);
    Path snapshot = classDir.resolve("render.html");
    if (exists) {
      Files.writeString(snapshot, "<p>committed</p>");
    }
    Path orphan = Files.writeString(classDir.resolve("obsolete.html"), "<p>orphan</p>");

    TestExecutionSummary summary = launch(fixture);

    assertThat(summary.getTestsFailedCount()).isEqualTo(1);
    assertThat(summary.getFailures())
        .singleElement()
        .satisfies(
            failure -> {
              assertThat(failure.getException())
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("snapshot.ci")
                  .hasMessageContaining(annotationUpdate ? "update = true" : "snapshot.update");
              if (annotationUpdate) {
                assertThat(failure.getException())
                    .hasMessageContaining(fixture.getName() + "#render");
              }
            });
    if (exists) {
      assertThat(snapshot).content().isEqualTo("<p>committed</p>");
    } else {
      assertThat(snapshot).doesNotExist();
    }
    assertThat(orphan).content().isEqualTo("<p>orphan</p>");
  }

  @Test
  void rejectsAnnotationUpdateEvenWithoutSnapshotAssertion() {
    TestExecutionSummary summary = launch(NoAssertionFixture.class);

    assertThat(summary.getTestsFailedCount()).isEqualTo(1);
    assertThat(summary.getFailures())
        .singleElement()
        .satisfies(
            failure ->
                assertThat(failure.getException())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("update = true"));
  }

  @Test
  void doesNotDeleteOrphansWhenUpdateIsEnabledAfterTest() throws IOException {
    Path classDir = fixtureClassDir(UpdateAfterTestFixture.class);
    Path snapshot = Files.writeString(classDir.resolve("render.html"), "<p>committed</p>");
    Path orphan = Files.writeString(classDir.resolve("obsolete.html"), "<p>orphan</p>");

    TestExecutionSummary summary = launch(UpdateAfterTestFixture.class);

    assertThat(summary.getTestsSucceededCount()).isEqualTo(1);
    assertThat(summary.getTotalFailureCount()).isZero();
    assertThat(snapshot).content().isEqualTo("<p>committed</p>");
    assertThat(orphan).content().isEqualTo("<p>orphan</p>");
  }

  private Path fixtureClassDir(Class<?> fixtureClass) throws IOException {
    return Files.createDirectories(
        baseDir.resolve(ResolvedConfig.DEFAULT_SNAPSHOT_DIR).resolve(fixtureClass.getName()));
  }

  private static TestExecutionSummary launch(Class<?> fixture) {
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    LauncherFactory.create()
        .execute(
            LauncherDiscoveryRequestBuilder.request().selectors(selectClass(fixture)).build(),
            listener);
    return listener.getSummary();
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class GlobalUpdateFixture {
    @SnapshotTest(inlineTemplate = "<p>changed</p>")
    void render(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class AnnotationUpdateFixture {
    @SnapshotTest(inlineTemplate = "<p>changed</p>", update = true)
    void render(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
    }
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class NoAssertionFixture {
    @SnapshotTest(inlineTemplate = "<p>changed</p>", update = true)
    void render() {}
  }

  @ExtendWith(ThymeleafSnapshotExtension.class)
  @EnabledIfSystemProperty(named = FIXTURE_FLAG, matches = "true")
  static class UpdateAfterTestFixture {
    @SnapshotTest(inlineTemplate = "<p>committed</p>")
    void render(Snapshot snapshot) {
      snapshot.assertMatchesSnapshot();
      System.setProperty(SnapshotProperties.UPDATE, "true");
    }
  }
}
