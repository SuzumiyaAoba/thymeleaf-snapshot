package com.github.suzumiyaaoba.thymeleaf.snapshot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

/**
 * Marks a test method as a Thymeleaf snapshot test.
 *
 * <p>This annotation combines {@link Test @Test} with snapshot test configuration.
 * It specifies which Thymeleaf template to render and compare against a stored snapshot.</p>
 *
 * <p>Exactly one of {@link #template()} or {@link #inlineTemplate()} must be specified.
 * Specifying both or neither will result in an {@link IllegalStateException} at test time.</p>
 *
 * <pre>{@code
 * @SnapshotTest(template = "pages/home")
 * void shouldRenderHomePage(Snapshot snapshot) {
 *     snapshot.setVariable("title", "Hello");
 *     snapshot.assertMatchesSnapshot();
 * }
 * }</pre>
 *
 * <p>For inline templates:</p>
 * <pre>{@code
 * @SnapshotTest(inlineTemplate = "<p th:text=\"${msg}\">placeholder</p>")
 * void shouldRenderInline(Snapshot snapshot) {
 *     snapshot.setVariable("msg", "Hello!");
 *     snapshot.assertMatchesSnapshot();
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Test
public @interface SnapshotTest {

    /**
     * The template name to render. Resolved using the configured template prefix and suffix.
     * Mutually exclusive with {@link #inlineTemplate()}.
     *
     * @return the template name, or empty string if not specified
     */
    String template() default "";

    /**
     * An inline template string to render directly.
     * Mutually exclusive with {@link #template()}.
     *
     * @return the inline template string, or empty string if not specified
     */
    String inlineTemplate() default "";

    /**
     * Whether to force-update the snapshot for this test.
     * When set to {@code true}, the stored snapshot is overwritten with the current
     * rendering result regardless of whether it matches.
     *
     * <p><strong>Note:</strong> This should only be used during development.
     * The global system property {@code -Dsnapshot.update=true} can also be used
     * to update all snapshots at once.</p>
     *
     * @return {@code true} if the snapshot should be updated
     */
    boolean update() default false;
}
