# Nano Vaadin Jetty

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

```java
import com.svenruppert.vaadin.nano.CoreUIServiceJava;

// 1. Plain start — no routes registered, caller wires up routes elsewhere
//    (e.g. via a META-INF/services VaadinServiceInitListener inside a JAR)
Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080);

// 2. Start with explicit routes — registered race-free, before the first request
Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080,
    List.of(MyView.class, AdminView.class));

// 3. Start with scanned routes — classgraph walks the given packages on the
//    runtime classpath for classes annotated @Route extending Component
Server server = CoreUIServiceJava.startServer("127.0.0.1", 8080,
    CoreUIServiceJava.scanForRoutes("com.example.views", "com.example.admin"));
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

## Releasing to Maven Central

Deploy targets are inherited from the parent
`com.svenruppert:dependencies:06.02.00`. Two paths exist:

- **`mvn deploy`** (no profile) goes to the legacy OSSRH endpoint
  `https://s01.oss.sonatype.org/...`. **OSSRH was shut down on 2025-06-30**;
  this path no longer works and is only kept for archival reasons.
- **`mvn deploy -P _deploy`** activates `central-publishing-maven-plugin`
  and publishes to the **Sonatype Central Portal**
  (`central.sonatype.com`) — the current replacement for OSSRH.

For an actual release to Central use the bundled profiles together:

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

## License

Distributed under the **European Union Public Licence v1.2** (SPDX:
`EUPL-1.2`). See `LICENSE` for the full text.
