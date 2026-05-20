# thymeleaf-snapshot

Snapshot testing library for [Thymeleaf](https://www.thymeleaf.org/) template rendering, integrated with [JUnit 5](https://junit.org/junit5/).

On first run, rendered output is saved as a snapshot file. Subsequent runs compare the output against that file and fail if they differ — catching unintended template regressions.

## Installation

### JitPack (Gradle)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    testImplementation 'com.github.SuzumiyaAoba:thymeleaf-snapshot:main-SNAPSHOT'
}
```

### Local publish

```bash
./gradlew publishToMavenLocal
```

```groovy
dependencies {
    testImplementation 'com.github.suzumiyaaoba:thymeleaf-snapshot:0.1.0-SNAPSHOT'
}
```

## Quick Start

```java
@ExtendWith(ThymeleafSnapshotExtension.class)
class HomePageTest {

    @SnapshotTest(template = "pages/home")
    void shouldRenderHomePage(Snapshot snapshot) {
        snapshot.setVariable("title", "Hello World");
        snapshot.assertMatchesSnapshot();
    }
}
```

Templates are resolved from the classpath under `templates/` with a `.html` suffix by default (configurable via `@SnapshotConfig`).

## Features

### Classpath and inline templates

```java
// Classpath template: resolves templates/pages/home.html
@SnapshotTest(template = "pages/home")
void classpathTemplate(Snapshot snapshot) { ... }

// Inline template string
@SnapshotTest(inlineTemplate = "<p th:text=\"${msg}\">placeholder</p>")
void inlineTemplate(Snapshot snapshot) {
    snapshot.setVariable("msg", "Hello!");
    snapshot.assertMatchesSnapshot();
}
```

### Multiple snapshots per test

```java
@SnapshotTest(template = "pages/dashboard")
void shouldRenderMultipleStates(Snapshot snapshot) {
    snapshot.setVariable("loggedIn", true);
    snapshot.assertMatchesSnapshot("logged-in");

    snapshot.clearVariables();
    snapshot.setVariable("loggedIn", false);
    snapshot.assertMatchesSnapshot("logged-out");
}
```

### Fluent variable API

```java
snapshot
    .setVariable("title", "My Page")
    .setVariable("items", List.of("A", "B"))
    .assertMatchesSnapshot();
```

### Updating snapshots

When the template output intentionally changes, update the stored snapshots:

```bash
# Update all snapshots
./gradlew test -Dsnapshot.update=true

# Update a single test's snapshot via annotation
@SnapshotTest(template = "pages/home", update = true)
```

> **Note:** Remove `update = true` before committing — it suppresses assertion failures.

## Configuration

Apply `@SnapshotConfig` to a test class to override defaults:

```java
@ExtendWith(ThymeleafSnapshotExtension.class)
@SnapshotConfig(
    templatePrefix = "templates/",
    templateSuffix = ".html",
    snapshotDir = "__snapshots__",
    prettyPrint = true,
    characterEncoding = "UTF-8"
)
class MyTest { ... }
```

| Attribute | Default | Description |
|---|---|---|
| `templatePrefix` | `templates/` | Classpath prefix for template resolution |
| `templateSuffix` | `.html` | Suffix appended to template names |
| `snapshotDir` | `__snapshots__` | Snapshot directory under `src/test/resources/` |
| `prettyPrint` | `false` | Format HTML with Jsoup before storing |
| `characterEncoding` | `UTF-8` | Template character encoding |

## System properties

| Property | Description |
|---|---|
| `snapshot.update` | Set to `true` to update all snapshot files |
| `snapshot.baseDir` | Override the snapshot root directory (defaults to `src/test/resources/`) |

Example:
```bash
./gradlew test -Dsnapshot.baseDir=/tmp/snapshots
```

## Snapshot file layout

```
src/test/resources/
  __snapshots__/
    com.example.HomePageTest/
      shouldRenderHomePage.html
      shouldRenderMultipleStates[logged-in].html
      shouldRenderMultipleStates[logged-out].html
```

## License

[MIT](https://opensource.org/licenses/MIT)
