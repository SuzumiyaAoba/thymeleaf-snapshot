package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedConfigTest {

    @SnapshotConfig(
            templatePrefix = "custom/",
            templateSuffix = ".htm",
            snapshotDir = "custom-snaps",
            prettyPrint = true,
            characterEncoding = "UTF-16"
    )
    private static final class AnnotatedClass {}

    private static void assertIsDefaults(ResolvedConfig config) {
        assertThat(config.templatePrefix()).isEqualTo(ResolvedConfig.DEFAULT_TEMPLATE_PREFIX);
        assertThat(config.templateSuffix()).isEqualTo(ResolvedConfig.DEFAULT_TEMPLATE_SUFFIX);
        assertThat(config.snapshotDir()).isEqualTo(ResolvedConfig.DEFAULT_SNAPSHOT_DIR);
        assertThat(config.prettyPrint()).isFalse();
        assertThat(config.characterEncoding()).isEqualTo(ResolvedConfig.DEFAULT_CHARACTER_ENCODING);
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
    }

    @Test
    void defaults_matchConstants() {
        assertIsDefaults(ResolvedConfig.defaults());
    }
}
