# Release Notes — `nano-vaadin-jetty 04.00.00`

This is a **major release**. `CoreUIServiceJava.startServer(...)` no
longer throws — it returns `Result<Server, Exception>`. See the Breaking
Change section below for the one-line migration; everything else in
this release is additive or internal cleanup.

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

- **Fail-fast bundle check**: `startServer(...)` probes the classpath for `META-INF/VAADIN/webapp/index.html` before booting Jetty. If the bundle is missing, the call short-circuits to `Result.failure(IllegalStateException)` with a message naming the expected path and pointing at `vaadin-maven-plugin build-frontend`. **No more Vaadin HTTP 500 "Unable to find index.html" on the first request** — misconfiguration surfaces at startup.
- **New 4-arg `startServer(host, port, routes, ClassLoader)` overload** lets callers supply an explicit ClassLoader for the bundle probe. The 3-arg form delegates with the library's own loader; the 4-arg form is for modular/shaded/plugin-isolated setups (and is what the test suite uses to exercise the missing-bundle branch with an empty `URLClassLoader`).
- Drop `ensureProductionTokenFile()` (used to write `flow-build-info.json` into `target/classes` at runtime). The library no longer ships any token file — frontend setup (`vaadin-maven-plugin` `prepare-frontend` / `build-frontend`) is the consumer's responsibility, matching Vaadin's standard expectation.
- `scanForRoutes` calls `loadClass(false)` — `@Route` classes are no longer eagerly initialised before Vaadin bootstrap.
- Remove four redundant calls (`setContextPath("/")`, `setConfigurationDiscovered(true)`, `addConfiguration(new AnnotationConfiguration())`, `setAsyncSupported(true)`). All four are Jetty 12.1 EE11 defaults; PIT confirmed equivalence after removal.
- Replace deprecated `setLogUrlOnStart(true)` with an explicit `HasLogger.staticLogger().info("Listening on http://{}:{}/", …)` after `server.start()`.

## Build & Dependencies

- **Parent bump**: `com.svenruppert:dependencies 06.02.00` → `06.02.02`.
- **SLF4J downgrade**: `slf4j-api` / `slf4j-simple` pinned to `2.0.17` (stable) — parent BOM ships `2.1.0-alpha1`, an alpha release that has no business on a Central-published library's public API.
- **SLF4J binding scope**: `slf4j-simple` overridden to `test` scope. The library no longer pins its consumers to a specific SLF4J binding.
- **Jackson annotations pin** (`com.fasterxml.jackson.core:jackson-annotations:2.21`) explicitly retained and documented — defends against the `3.0-rc5` that `jackson-databind:3.1.x` pulls in transitively and which breaks `MapperBuilder.<clinit>`.
- **Demo bundle stays out of the published jar**: the `demo` profile builds the Vaadin React frontend into `target/classes/META-INF/VAADIN/` so `exec:java` can serve it, but a profile-scoped `maven-jar-plugin` exclude (`META-INF/VAADIN/**`) keeps the resulting artefact small. **Demo jar: 6.7 MB → 9.4 KB.** Library-mode jar (no profile) was unaffected at ~9 KB.
- Removed fossil `dependency-reduced-pom.xml` at repo root (legacy from a removed shade configuration).

## Tooling

- **Mutation testing wired in**. PIT scoped to `com.svenruppert.vaadin.nano.*` (production) / `demo.*` (tests); overrides the parent default `junit.com.svenruppert.*` that would not match this module.
  - **Mutation coverage: 100%** (22/22 killed, 0 NO_COVERAGE, Test Strength 100%).
  - PIT runtime ~17 s on a developer laptop.
  - Run with `./mvnw org.pitest:pitest-maven:mutationCoverage`; report lands in `target/pit-reports/index.html`.
- **Release helper scripts** under `scripts/`:
  - `scripts/publish-to-central.sh` — one-shot path: pre-flight, `clean test`, PIT mutation gate (must hit 100%), then `mvn -P _deploy,_release_prepare,_release_sign-artifacts deploy`. Uploads via `central-publishing-maven-plugin`.
  - `scripts/build-central-bundle.sh` — local-only path: builds + signs the artefacts, stages them in the Maven-Central layout (`groupId/artifactId/version/`) with `.md5` / `.sha1` / `.sha256` / `.sha512` checksums, zips to `target/central-bundle-<version>.zip`. No upload — drop the ZIP on <https://central.sonatype.com/publishing/deployments>.
  - Both share pre-flight (GPG key, non-`-SNAPSHOT` version), `xmllint`-based coordinate extraction (no Maven JVM cold-start), and `--help`.
- `src/test/resources/simplelogger.properties` dampens Atmosphere/Jetty `INFO` chatter to `WARN`. Test output dropped from ~25 noise lines to zero.
- `license-maven-plugin` disabled in this module (parent forces `eupl_v1_1` template, plugin has no `eupl_v1_2` built-in). Header convention now documented in README.

## Tests

- `CoreUIServiceJavaTest` grown from 1 to 7 cases:
  - explicit requested port propagation
  - port-collision failure path (`Result.isFailure()` proof)
  - explicit-route registration end-to-end
  - `scanForRoutes` happy path + empty-package path
  - **`failsLoudlyWhenFrontendBundleIsMissing`** — drives the new bundle-probe failure branch with an empty `URLClassLoader`; asserts both the exception type and the message keywords (`index.html`, `build-frontend`)
- New stub `src/test/resources/META-INF/VAADIN/webapp/index.html` keeps the happy-path tests independent of an actual Vite build.
- `DemoViewBrowserlessTest` unchanged.
- Total: 8 tests, all green.

## Internal

- All Java sources moved to EUPL 1.2 headers with the live `joinup.ec.europa.eu/software/page/eupl` URL (replacing the dead `ec.europa.eu/idabc/eupl5`).
- `DemoApplication` switches from `System.out.println` to `HasLogger.staticLogger()`.
- `com.svenruppert:core` promoted to `compile` scope (HasLogger is now used by library code, not only the demo).

## Documentation

- New `### Frontend bundle is the consumer's responsibility` section makes the build-frontend contract explicit; documents the new fail-fast probe and the 4-arg `ClassLoader` overload for non-standard loaders.
- New `### Handling startup failure` subsection of the Library API shows the `Result` lifecycle pattern (`peek` / `peekFailure`) for callers that prefer not to throw.
- New `### Release helper scripts` subsection of "Releasing to Maven Central" presents a side-by-side table: when to use `scripts/publish-to-central.sh` (uploads) vs. `scripts/build-central-bundle.sh` (local ZIP only).
- API examples updated to show the `Result` return type with `.getOrThrow()` for brevity.
- "Running the demo" notes the `maven-jar-plugin` exclude that keeps the artefact at ~9 KB even after `-P demo package`.
- New `## Conventions` section documents the license-plugin skip and the manual-header rule for new files.
- `## Releasing to Maven Central` block reduced to a Central-Portal-first deploy guide; OSSRH path (shut down 2025-06-30) kept as a one-line footer note.

---

**Compatibility**: Java 26, Vaadin 25.1.x, Jetty 12.1.x (EE11). No change in runtime requirements vs. the 03.x line.

**Source compatibility**: One breaking change — the `startServer` return type (`throws Exception` → `Result<Server, Exception>`). The new 4-arg `startServer(host, port, routes, ClassLoader)` is additive. All remaining changes are internal or build-only.

**Behaviour change**: a misconfigured caller (no frontend bundle on the classpath) now sees a `Result.failure(IllegalStateException)` at `startServer(...)` instead of Vaadin's HTTP 500 on the first request. If your build already runs `vaadin-maven-plugin build-frontend`, nothing changes for you.
