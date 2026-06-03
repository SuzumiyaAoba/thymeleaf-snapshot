package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SnapshotPropertiesTest {

  @AfterEach
  void clearSystemProperties() {
    System.clearProperty(SnapshotProperties.UPDATE);
    System.clearProperty(SnapshotProperties.CI);
    System.clearProperty(SnapshotProperties.BASE_DIR);
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
