package com.github.suzumiyaaoba.thymeleaf.snapshot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResolvedConfigTest {

    @SnapshotConfig(
            templatePrefix = "custom/",
            templateSuffix = ".htm",
            snapshotDir = "custom-snaps",
            prettyPrint = true,
            characterEncoding = "UTF-16"
    )
    private static final class AnnotatedClass {}

    @Test
    void from_null_returnsDefaults() {
        ResolvedConfig config = ResolvedConfig.from(null);

        assertEquals(ResolvedConfig.DEFAULT_TEMPLATE_PREFIX, config.templatePrefix());
        assertEquals(ResolvedConfig.DEFAULT_TEMPLATE_SUFFIX, config.templateSuffix());
        assertEquals(ResolvedConfig.DEFAULT_SNAPSHOT_DIR, config.snapshotDir());
        assertFalse(config.prettyPrint());
        assertEquals(ResolvedConfig.DEFAULT_CHARACTER_ENCODING, config.characterEncoding());
    }

    @Test
    void from_annotation_usesAnnotationValues() {
        SnapshotConfig annotation = AnnotatedClass.class.getAnnotation(SnapshotConfig.class);

        ResolvedConfig config = ResolvedConfig.from(annotation);

        assertEquals("custom/", config.templatePrefix());
        assertEquals(".htm", config.templateSuffix());
        assertEquals("custom-snaps", config.snapshotDir());
        assertTrue(config.prettyPrint());
        assertEquals("UTF-16", config.characterEncoding());
    }

    @Test
    void defaults_matchConstants() {
        ResolvedConfig config = ResolvedConfig.defaults();

        assertEquals(ResolvedConfig.DEFAULT_TEMPLATE_PREFIX, config.templatePrefix());
        assertEquals(ResolvedConfig.DEFAULT_TEMPLATE_SUFFIX, config.templateSuffix());
        assertEquals(ResolvedConfig.DEFAULT_SNAPSHOT_DIR, config.snapshotDir());
        assertFalse(config.prettyPrint());
        assertEquals(ResolvedConfig.DEFAULT_CHARACTER_ENCODING, config.characterEncoding());
    }
}
