package com.github.suzumiyaaoba.thymeleaf.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.thymeleaf.templatemode.TemplateMode;

class ResolvedConfigTest {

  @SnapshotConfig(
      templatePrefix = "custom/",
      templateSuffix = ".htm",
      snapshotDir = "custom-snaps",
      prettyPrint = true,
      characterEncoding = "UTF-16")
  private static final class AnnotatedClass {}

  @SnapshotConfig(templateMode = TemplateMode.XML)
  private static final class XmlAnnotatedClass {}

  private static void assertIsDefaults(ResolvedConfig config) {
    assertThat(config.templatePrefix()).isEqualTo(ResolvedConfig.DEFAULT_TEMPLATE_PREFIX);
    assertThat(config.templateSuffix()).isEqualTo(ResolvedConfig.DEFAULT_TEMPLATE_SUFFIX);
    assertThat(config.snapshotDir()).isEqualTo(ResolvedConfig.DEFAULT_SNAPSHOT_DIR);
    assertThat(config.prettyPrint()).isFalse();
    assertThat(config.characterEncoding()).isEqualTo(ResolvedConfig.DEFAULT_CHARACTER_ENCODING);
    assertThat(config.templateMode()).isEqualTo(TemplateMode.HTML);
  }

  @Test
  void from_null_returnsDefaults() {
    assertIsDefaults(ResolvedConfig.from(null));
  }

  @Test
  void from_annotation_usesAnnotationValues() {
    SnapshotConfig annotation = AnnotatedClass.class.getAnnotation(SnapshotConfig.class);

    ResolvedConfig config = ResolvedConfig.from(annotation);

    assertThat(config.templatePrefix()).isEqualTo("custom/");
    assertThat(config.templateSuffix()).isEqualTo(".htm");
    assertThat(config.snapshotDir()).isEqualTo("custom-snaps");
    assertThat(config.prettyPrint()).isTrue();
    assertThat(config.characterEncoding()).isEqualTo("UTF-16");
    assertThat(config.templateMode()).isEqualTo(TemplateMode.HTML);
  }

  @Test
  void from_annotation_usesXmlTemplateMode() {
    SnapshotConfig annotation = XmlAnnotatedClass.class.getAnnotation(SnapshotConfig.class);

    ResolvedConfig config = ResolvedConfig.from(annotation);

    assertThat(config.templateMode()).isEqualTo(TemplateMode.XML);
  }

  @Test
  void defaults_matchConstants() {
    assertIsDefaults(ResolvedConfig.defaults());
  }

  @Test
  void extensionForMode_html() {
    assertThat(ResolvedConfig.extensionForMode(TemplateMode.HTML)).isEqualTo(".html");
  }

  @Test
  void extensionForMode_xml() {
    assertThat(ResolvedConfig.extensionForMode(TemplateMode.XML)).isEqualTo(".xml");
  }

  @Test
  void extensionForMode_text() {
    assertThat(ResolvedConfig.extensionForMode(TemplateMode.TEXT)).isEqualTo(".txt");
  }

  @Test
  void extensionForMode_javascript() {
    assertThat(ResolvedConfig.extensionForMode(TemplateMode.JAVASCRIPT)).isEqualTo(".js");
  }

  @Test
  void extensionForMode_css() {
    assertThat(ResolvedConfig.extensionForMode(TemplateMode.CSS)).isEqualTo(".css");
  }

  @Test
  void extensionForMode_raw() {
    assertThat(ResolvedConfig.extensionForMode(TemplateMode.RAW)).isEqualTo(".txt");
  }
}
