verschiebe die Skripte in ein Verzeichnis scripts# Nano Vaadin Jetty

A small Java 26 / Vaadin Flow launcher on embedded Jetty 12 (EE11). The library
exposes a tiny API to spin up a Jetty server with a Vaadin servlet attached,
plus helpers to register `@Route` classes — both explicitly and by scanning
the runtime classpath.

## Requirements

- Java **26**
- Maven **4.0.0-rc-5** (via the included `./mvnw` wrapper)

## Project layout

Single-module Maven project. The library lives under
`com.svenruppert.vaadin.nano`:

- `src/main/java/com/svenruppert/vaadin/nano/` — `CoreUIServiceJava`,
  `RouteRegistrationListener`
- `src/test/java/demo/` — runnable demo (`DemoApplication`, `DemoView`) and tests

Vaadin UI dependencies (`vaadin-button-flow`, `vaadin-ordered-layout-flow`,
`flow-html-components`, `vaadin`) are scoped to **test** by default so the
library jar stays minimal. The `demo` profile elevates them to `compile`
so the demo can actually use them and the `vaadin-maven-plugin` can build a
production frontend bundle.

## Library API

`startServer(...)` returns `Result<Server, Exception>` (from
`com.svenruppert:functional-reactive`); the examples below use
`getOrThrow()` for brevity. See [Handling startup
failure](#handling-startup-failure) below for the non-throwing form.

```java
import com.svenruppert.vaadin.nano.CoreUIServiceJava;

// 1. Plain start — no routes registered, caller wires up routes elsewhere
//    (e.g. via a META-INF/services VaadinServiceInitListener inside a JAR)
Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080).getOrThrow();

// 2. Start with explicit routes — registered race-free, before the first request
Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080,
    List.of(MyView.class, AdminView.class)).getOrThrow();

// 3. Start with scanned routes — classgraph walks the given packages on the
//    runtime classpath for classes annotated @Route extending Component
Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080,
    CoreUIServiceJava.scanForRoutes("com.example.views", "com.example.admin"))
    .getOrThrow();
```

### Handling startup failure

`Result` makes a failed startup (`BindException`, missing config, …) a
first-class return value instead of a thrown checked exception:

```java
CoreUIServiceJava.startServer("127.0.0.1", 8080)
    .peek(srv -> logger().info("listening on {}", srv.getURI()))
    .peekFailure(err -> logger().error("startup failed", err));
```

### Why an explicit registration step?

Embedded Jetty's annotation scanner only walks JARs (filtered by
`CONTAINER_JAR_PATTERN`/`WEBINF_JAR_PATTERN`) and `WEB-INF/classes`. When the
app runs from an IDE or `mvn exec:java`, `@Route` classes live in plain
classpath directories (`target/classes`, `target/test-classes`) — Jetty's
scanner does not see them, so Vaadin's built-in `RouteRegistryInitializer`
gets an empty class set. The startServer overload installs a
`ServletContextListener` that registers the given routes against
`ApplicationRouteRegistry` during context initialization, before any request
can be served.

Views packaged inside JARs are still picked up by Vaadin's standard scanner
and need no manual registration.

### Frontend bundle is the consumer's responsibility

The library does **not** ship its own `flow-build-info.json` or a
prebuilt frontend bundle. Consumers wire `vaadin-maven-plugin` into
their own build (or use the `demo` profile in this repo) so that
`prepare-frontend` / `build-frontend` produce both the token file and
the bundle under `META-INF/VAADIN/`.

`startServer(...)` probes the classpath for
`META-INF/VAADIN/webapp/index.html` before booting Jetty. If the
bundle is missing, the call short-circuits to
`Result.failure(IllegalStateException)` *immediately*, with a message
naming the missing path and pointing at `build-frontend`. You will not
see Vaadin's runtime HTTP 500 `Unable to find index.html` — the
failure surfaces at startup instead.

If you load the bundle from a non-standard ClassLoader (modular setups,
shaded jars, plugin isolation), use the 4-arg overload to pass that
loader for the probe.

## Build

```bash
./mvnw clean test       # library tests
./mvnw clean package    # library jar (no frontend bundle, no UI deps)
```

## Running the demo

The demo lives under `src/test/java/demo`. It needs the `demo` profile so
that (a) the Vaadin UI components land on the compile classpath and (b) the
`vaadin-maven-plugin` builds the frontend bundle into
`target/classes/META-INF/VAADIN/webapp/`.

```bash
./mvnw -P demo -DskipTests package
./mvnw -P demo exec:java \
    -Dexec.mainClass=demo.DemoApplication \
    -Dexec.classpathScope=test
```

In IntelliJ: tick **demo** under Maven → Profiles, then run `clean install`,
then launch `DemoApplication.main()`.

Open `http://127.0.0.1:8080/` — button + click counter.

The `demo` profile's `maven-jar-plugin` excludes `META-INF/VAADIN/**` from
the produced jar, so even after `-P demo package` the artefact stays at
~9 KB. The bundle still lives in `target/classes` for `exec:java`.

## Releasing to Maven Central

Deploy targets are inherited from the parent
`com.svenruppert:dependencies`. Releases go to the **Sonatype Central
Portal** (`central.sonatype.com`) via `central-publishing-maven-plugin`.
The bare Maven command is:

```bash
./mvnw -P _deploy,_release_prepare,_release_sign-artifacts deploy
```

- `_release_prepare` attaches the `-sources.jar` and `-javadoc.jar`
  (mandatory for Central)
- `_release_sign-artifacts` GPG-signs every artifact (mandatory for
  Central)
- `_deploy` runs the actual upload via the central-publishing plugin

Credentials must live in `~/.m2/settings.xml` under
`<server><id>central</id>...</server>` — username is the Central Portal
token name, password is the token value. The GPG signing key must be
available to `gpg` on the build host.

> The bare `mvn deploy` (no profile) targets the legacy OSSRH endpoint
> at `s01.oss.sonatype.org`, which was shut down on 2025-06-30 — that
> path no longer works. Always use the `_deploy` profile.

### Release helper scripts

Two wrappers under [`scripts/`](scripts/) cover the two release paths:

| Script | What it does | Uploads? | Use it when |
|---|---|---|---|
| [`scripts/publish-to-central.sh`](scripts/publish-to-central.sh) | Pre-flight, runs `clean test`, runs the PIT mutation gate (must hit 100 %), then `mvn -P _deploy,_release_prepare,_release_sign-artifacts deploy` | **Yes** — via central-publishing-maven-plugin | You trust the build, you want a one-shot release, your `settings.xml` is set up |
| [`scripts/build-central-bundle.sh`](scripts/build-central-bundle.sh) | Pre-flight, builds + signs the artefacts, stages them in the Maven-Central layout, generates `.md5` / `.sha1` / `.sha256` / `.sha512`, zips it to `target/central-bundle-<version>.zip` | **No** — local ZIP only | You want to inspect the bundle (`unzip -l …`), verify signatures manually, or upload by hand at <https://central.sonatype.com/publishing/deployments> |

Both share the same pre-flight (`gpg` key present, non-`-SNAPSHOT`
version) and read the project coordinates straight from `pom.xml` via
`xmllint` to avoid Maven JVM cold-start latency. Run either with
`--help` for the full flag list.

Common flags:

- `--allow-snapshot` lets either script run against a `-SNAPSHOT`
  version (intended for dry runs / inspection only — Central rejects
  uploaded snapshots).
- `--dry-run` (publish only) skips the actual `deploy` step.
- `--skip-tests` / `--skip-mutation` (publish only) bypass the
  respective quality gate.
- `--keep-workdir` (bundle only) leaves the staging directory in place
  for inspection.

## Conventions

### License headers

All Java sources carry the EUPL 1.2 header. The parent
`com.svenruppert:dependencies` runs
`license-maven-plugin:update-file-header` in `process-sources` with
`<licenseName>eupl_v1_1</licenseName>` — and the plugin (2.7.1) has no
built-in `eupl_v1_2` template. This module therefore disables the
plugin via `<skip>true</skip>` and an unbound execution phase, so the
canonical 1.2 headers in `src/` stay intact. When adding a new Java
file, hand-copy the header from an existing one rather than relying on
the plugin to generate it.

## License

Distributed under the **European Union Public Licence v1.2** (SPDX:
`EUPL-1.2`). See `LICENSE` for the full text.