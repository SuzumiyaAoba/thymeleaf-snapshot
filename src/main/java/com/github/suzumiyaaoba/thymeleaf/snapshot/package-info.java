/**
 * Thymeleaf Snapshot Testing — a library for snapshot-based regression testing
 * of Thymeleaf template rendering, integrated with JUnit 5.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * @ExtendWith(ThymeleafSnapshotExtension.class)
 * class MyTemplateTest {
 *
 *     @SnapshotTest(template = "pages/home")
 *     void shouldRenderHomePage(Snapshot snapshot) {
 *         snapshot.setVariable("title", "Hello World");
 *         snapshot.assertMatchesSnapshot();
 *     }
 * }
 * }</pre>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.github.suzumiyaaoba.thymeleaf.snapshot.ThymeleafSnapshotExtension} —
 *       JUnit 5 extension that manages the snapshot test lifecycle</li>
 *   <li>{@link com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotTest} —
 *       annotation that marks a test method as a snapshot test</li>
 *   <li>{@link com.github.suzumiyaaoba.thymeleaf.snapshot.Snapshot} —
 *       user-facing API for setting variables and asserting snapshots</li>
 *   <li>{@link com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotConfig} —
 *       optional class-level configuration annotation</li>
 * </ul>
 *
 * @see com.github.suzumiyaaoba.thymeleaf.snapshot.ThymeleafSnapshotExtension
 * @see com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotTest
 */
package com.github.suzumiyaaoba.thymeleaf.snapshot;
