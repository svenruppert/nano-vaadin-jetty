# Release Notes — `nano-vaadin-jetty 03.00.01`

## ⚠️ Breaking Change

**`CoreUIServiceJava.startServer(...)` returns `Result<Server, Exception>` instead of throwing `Exception`.**

Migration:

```diff
- Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080);
+ Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080).getOrThrow();
```

For lifecycle code that wants to react to failure without exceptions:

```java
CoreUIServiceJava.startServer(host, port)
    .peek(server -> logger.info("up"))
    .peekFailure(err -> logger.error("startup failed", err));
```

Requires `com.svenruppert:functional-reactive` (transitive via parent BOM).

---

## Library API

- Drop `ensureProductionTokenFile()` (used to write `flow-build-info.json` into `target/classes` at runtime). The library no longer ships any token file — frontend setup (`vaadin-maven-plugin` `prepare-frontend` / `build-frontend`) is the consumer's responsibility, matching Vaadin's standard expectation.
- `scanForRoutes` calls `loadClass(false)` — `@Route` classes are no longer eagerly initialised before Vaadin bootstrap.
- Remove four redundant calls (`setContextPath("/")`, `setConfigurationDiscovered(true)`, `addConfiguration(new AnnotationConfiguration())`, `setAsyncSupported(true)`). All four are Jetty 12.1 EE11 defaults; PIT confirmed equivalence after removal.
- Replace deprecated `setLogUrlOnStart(true)` with an explicit `HasLogger.staticLogger().info("Listening on http://{}:{}/", …)` after `server.start()`.

## Build & Dependencies

- **Parent bump**: `com.svenruppert:dependencies 06.02.00` → `06.02.02`.
- **SLF4J downgrade**: `slf4j-api` / `slf4j-simple` pinned to `2.0.17` (stable) — parent BOM ships `2.1.0-alpha1`, an alpha release that has no business on a Central-published library's public API.
- **SLF4J binding scope**: `slf4j-simple` overridden to `test` scope. The library no longer pins its consumers to a specific SLF4J binding.
- **Jackson annotations pin** (`com.fasterxml.jackson.core:jackson-annotations:2.21`) explicitly retained and documented — defends against the `3.0-rc5` that `jackson-databind:3.1.x` pulls in transitively and which breaks `MapperBuilder.<clinit>`.
- Removed fossil `dependency-reduced-pom.xml` at repo root (legacy from a removed shade configuration).

## Tooling

- **Mutation testing wired in**. PIT scoped to `com.svenruppert.vaadin.nano.*` (production) / `demo.*` (tests); overrides the parent default `junit.com.svenruppert.*` that would not match this module.
  - **Mutation coverage: 100%** (19/19 killed, 0 NO_COVERAGE, Test Strength 100%).
  - PIT runtime ~18 s on a developer laptop.
  - Run with `./mvnw org.pitest:pitest-maven:mutationCoverage`; report lands in `target/pit-reports/index.html`.
- `src/test/resources/simplelogger.properties` dampens Atmosphere/Jetty `INFO` chatter to `WARN`. Test output dropped from ~25 noise lines to zero.
- `license-maven-plugin` disabled in this module (parent forces `eupl_v1_1` template, plugin has no `eupl_v1_2` built-in). Header convention now documented in README.

## Tests

- `CoreUIServiceJavaTest` grown from 1 to 6 cases:
  - explicit requested port propagation
  - port-collision failure path (`Result.isFailure()` proof)
  - explicit-route registration end-to-end
  - `scanForRoutes` happy path + empty-package path
- `DemoViewBrowserlessTest` unchanged.
- Total: 7 tests, all green.

## Internal

- All Java sources moved to EUPL 1.2 headers with the live `joinup.ec.europa.eu/software/page/eupl` URL (replacing the dead `ec.europa.eu/idabc/eupl5`).
- `DemoApplication` switches from `System.out.println` to `HasLogger.staticLogger()`.
- `com.svenruppert:core` promoted to `compile` scope (HasLogger is now used by library code, not only the demo).

## Documentation

- New `### Frontend bundle is the consumer's responsibility` section makes the build-frontend contract explicit and names the 500 symptom for diagnosis.
- New `## Conventions` section documents the license-plugin skip and the manual-header rule for new files.
- `## Releasing to Maven Central` block reduced to a Central-Portal-first deploy guide; OSSRH path (shut down 2025-06-30) kept as a one-line footer note.

---

**Compatibility**: Java 26, Vaadin 25.1.x, Jetty 12.1.x (EE11). No change in runtime requirements vs. 03.00.00.

**Source compatibility**: One breaking change — the `startServer` return type. All other changes are internal or build-only.
