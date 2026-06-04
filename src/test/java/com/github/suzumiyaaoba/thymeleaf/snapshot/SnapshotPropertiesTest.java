package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SnapshotPropertiesTest {

  private Properties savedProperties;

  @BeforeEach
  void clearProperties() {
    // Snapshot the global state and start from a clean slate so the "default" assertions are
    // deterministic regardless of any -Dsnapshot.* flags on the launching command line.
    savedProperties = (Properties) System.getProperties().clone();
    System.clearProperty(SnapshotProperties.UPDATE);
    System.clearProperty(SnapshotProperties.CI);
    System.clearProperty(SnapshotProperties.BASE_DIR);
  }

  @AfterEach
  void restoreProperties() {
    System.setProperties(savedProperties);
  }

  @Test
  void isUpdateEnabledReflectsSystemProperty() {
    assertThat(SnapshotProperties.isUpdateEnabled()).isFalse();
    System.setProperty(SnapshotProperties.UPDATE, "true");
    assertThat(SnapshotProperties.isUpdateEnabled()).isTrue();
  }

  @Test
  void isCiEnabledReflectsSystemProperty() {
    assertThat(SnapshotProperties.isCiEnabled()).isFalse();
    System.setProperty(SnapshotProperties.CI, "true");
    assertThat(SnapshotProperties.isCiEnabled()).isTrue();
  }

  @Test
  void baseDirOverrideReturnsNullWhenUnset() {
    assertThat(SnapshotProperties.baseDirOverride()).isNull();
  }

  @Test
  void baseDirOverrideReturnsNullWhenBlank() {
    System.setProperty(SnapshotProperties.BASE_DIR, "   ");
    assertThat(SnapshotProperties.baseDirOverride()).isNull();
  }

  @Test
  void baseDirOverrideReturnsValueWhenSet() {
    System.setProperty(SnapshotProperties.BASE_DIR, "/tmp/snap");
    assertThat(SnapshotProperties.baseDirOverride()).isEqualTo("/tmp/snap");
  }
}
