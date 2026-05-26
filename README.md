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
        snapshot
            .setVariable("title", "Hello World")
            .assertMatchesSnapshot();
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
    snapshot.setVariable("msg", "Hello!").assertMatchesSnapshot();
}
```

### Multiple snapshots per test

```java
@SnapshotTest(template = "pages/dashboard")
void shouldRenderMultipleStates(Snapshot snapshot) {
    snapshot
        .setVariable("loggedIn", true)
        .assertMatchesSnapshot("logged-in");

    snapshot
        .setVariable("loggedIn", false)
        .assertMatchesSnapshot("logged-out");
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

## Sample

Working examples are in the [`sample/`](sample/) subproject. Run them with:

```bash
./gradlew :sample:test
```

### [ProductListTest](sample/src/test/java/com/github/suzumiyaaoba/thymeleaf/snapshot/sample/ProductListTest.java)

Renders a product list from a POJO list. Uses `prettyPrint = true` so the stored snapshot is human-readable.

```java
@ExtendWith(ThymeleafSnapshotExtension.class)
@SnapshotConfig(prettyPrint = true)
class ProductListTest {

    @SnapshotTest(template = "product-list")
    void shouldRenderProductList(Snapshot snapshot) {
        var products = List.of(
                new Product("Widget Pro", "$49.99", true),
                new Product("Gadget Max", "$129.00", true),
                new Product("Doohickey", "$9.99", false)
        );
        snapshot.setVariable("pageTitle", "Our Products")
                .setVariable("products", products)
                .assertMatchesSnapshot();
    }
}
```

### [UserProfileTest](sample/src/test/java/com/github/suzumiyaaoba/thymeleaf/snapshot/sample/UserProfileTest.java)

Asserts two rendering states (admin / member) from a single test method using named snapshots and `clearVariables()`.

```java
@SnapshotTest(template = "user-profile")
void shouldRenderDifferentRoles(Snapshot snapshot) {
    snapshot.setVariable("user", new User("Alice", "Platform engineer", "admin"))
            .assertMatchesSnapshot("admin");

    snapshot.clearVariables()
            .setVariable("user", new User("Bob", "Open-source contributor", "member"))
            .assertMatchesSnapshot("member");
}
```

Produces two snapshot files:
```
__snapshots__/...UserProfileTest/shouldRenderDifferentRoles[admin].html
__snapshots__/...UserProfileTest/shouldRenderDifferentRoles[member].html
```

### [WelcomeEmailTest](sample/src/test/java/com/github/suzumiyaaoba/thymeleaf/snapshot/sample/WelcomeEmailTest.java)

Embeds the template string directly in the annotation — useful for small fragments or email bodies.

```java
@SnapshotTest(inlineTemplate = """
        <div class="email">
          <h2 th:text="|Welcome, ${name}!|">Welcome!</h2>
          <a th:href="${activationUrl}" class="cta">Activate Account</a>
        </div>
        """)
void shouldRenderWelcomeEmail(Snapshot snapshot) {
    snapshot.setVariable("name", "Alice")
            .setVariable("activationUrl", "https://example.com/activate/abc123")
            .assertMatchesSnapshot();
}
```

## License

[MIT](https://opensource.org/licenses/MIT)
