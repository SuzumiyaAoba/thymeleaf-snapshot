# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and test
./gradlew test

# Single test class
./gradlew test --tests "com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotMismatchExceptionTest"

# Single test method
./gradlew test --tests "com.github.suzumiyaaoba.thymeleaf.snapshot.SnapshotMismatchExceptionTest.shouldContainDiffInMessage"

# Build without tests
./gradlew build -x test

# Update all snapshots
./gradlew test -Dsnapshot.update=true

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

## Architecture

This is a JUnit 5 extension library for snapshot testing of Thymeleaf templates. The flow is:

1. **`ThymeleafSnapshotExtension`** (JUnit 5 `BeforeEachCallback` + `ParameterResolver`) — entry point. Reads `@SnapshotConfig` from the test class, constructs `ThymeleafRenderer` and `SnapshotManager` (cached at class level in the JUnit `ExtensionContext` store), then creates a per-test `Snapshot` instance and injects it into the test method parameter.

2. **`Snapshot`** — user-facing API. Holds template variables and locale, calls `ThymeleafRenderer` to render, optionally pretty-prints via `HtmlFormatter`, then delegates to `SnapshotManager` for file comparison or creation.

3. **`SnapshotManager`** — handles all file I/O. On construction with a `String` dir name, auto-detects the project root by walking up from the classloader resource URL looking for `build.gradle`/`pom.xml`. Overridable via `-Dsnapshot.baseDir=...`. Snapshot files live at `src/test/resources/__snapshots__/<FQCN>/<method>[<name>].html`.

4. **`ThymeleafRenderer`** — wraps two `TemplateEngine` instances: one with `ClassLoaderTemplateResolver` (for `@SnapshotTest(template=...)`) and one with `StringTemplateResolver` (for `@SnapshotTest(inlineTemplate=...)`). Both have caching disabled.

5. **`SnapshotMismatchException`** — extends `AssertionError` (JUnit recognises it as a test failure). Uses `java-diff-utils` (`DiffUtils.diff` + `UnifiedDiffUtils.generateUnifiedDiff`) to produce a standard unified diff with `@@ -L,C +L,C @@` hunk headers.

6. **`ResolvedConfig`** — record that merges `@SnapshotConfig` annotation values with defaults. Created fresh per-test-class; passed to both `ThymeleafRenderer` and `SnapshotManager`.

### Key design constraints

- `@SnapshotTest` is a composed annotation (`@Test` + custom metadata), so methods need only one annotation.
- Exactly one of `template` or `inlineTemplate` must be set; validated at `Snapshot` construction.
- Default locale is `Locale.ROOT` — do not change to `Locale.getDefault()` as that makes snapshots machine-dependent.
- `ThymeleafRenderer` and `SnapshotManager` are heavyweight; they are cached at the class-level `ExtensionContext` store to avoid re-creation per test method.
