package demo;

/*-
 * #%L
 * nano-vaadin-jetty
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2026 Vaadin
 * %%
 * Licensed under the EUPL, Version 1.2 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */

import com.svenruppert.functional.result.Result;
import com.svenruppert.vaadin.nano.CoreUIServiceJava;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreUIServiceJavaTest {

  // A classloader that exposes only the frontend-bundle probe target. Built once for the
  // whole test class and passed to the 4-arg startServer overload so happy-path tests stay
  // independent of the runtime classpath layout. Putting the stub on src/test/resources
  // would shadow the real bundle when the demo is launched via `exec:java
  // -Dexec.classpathScope=test` after `-P demo package` — see the deliberate omission of
  // META-INF/VAADIN/webapp/ under src/test/resources/.
  private static final ClassLoader BUNDLE_CL = bundleStubClassLoader();

  @Test
  void startsJettyOnEphemeralPortWithVaadinServlet() throws Exception {
    Result<Server, Exception> result =
        CoreUIServiceJava.startServer("127.0.0.1", 0, List.of(), BUNDLE_CL);
    assertTrue(result.isSuccess(), () -> "startServer failed: " + result);
    Server server = result.getOrThrow();
    try {
      assertTrue(server.isStarted());

      Connector connector = server.getConnectors()[0];
      ServerConnector serverConnector = assertInstanceOf(ServerConnector.class, connector);
      assertTrue(serverConnector.getLocalPort() > 0);
      assertEquals("127.0.0.1", serverConnector.getHost());

      WebAppContext webAppContext = assertInstanceOf(WebAppContext.class, server.getHandler());
      assertEquals("/", webAppContext.getContextPath());
      assertTrue(webAppContext.isConfigurationDiscovered(), "configurationDiscovered must be true");
      assertTrue(webAppContext.isParentLoaderPriority(), "parentLoaderPriority must be true");
      assertNotNull(webAppContext.getBaseResource(), "baseResource must be set");
      assertTrue(webAppContext.getConfigurations().toString().contains("AnnotationConfiguration"),
                 () -> "AnnotationConfiguration must be in the configuration chain, got: "
                       + webAppContext.getConfigurations());

      ServletHolder holder = webAppContext.getServletHandler().getServlets()[0];
      assertEquals("true", holder.getInitParameter("productionMode"));
      assertTrue(holder.isAsyncSupported());
      assertEquals(1, holder.getInitOrder());
    } finally {
      server.stop();
      server.destroy();
    }

    assertFalse(server.isRunning());
  }

  @Test
  void startsWithExplicitRequestedPort() throws Exception {
    int requestedPort = findFreePort();
    Result<Server, Exception> result =
        CoreUIServiceJava.startServer("127.0.0.1", requestedPort, List.of(), BUNDLE_CL);
    assertTrue(result.isSuccess());
    Server server = result.getOrThrow();
    try {
      ServerConnector connector = (ServerConnector) server.getConnectors()[0];
      assertEquals(requestedPort, connector.getLocalPort(),
                   "setPort must propagate the requested port to the connector");
    } finally {
      server.stop();
      server.destroy();
    }
  }

  @Test
  void returnsFailureWhenPortAlreadyBound() throws Exception {
    Result<Server, Exception> first =
        CoreUIServiceJava.startServer("127.0.0.1", 0, List.of(), BUNDLE_CL);
    Server occupier = first.getOrThrow();
    int boundPort = ((ServerConnector) occupier.getConnectors()[0]).getLocalPort();
    try {
      Result<Server, Exception> second =
          CoreUIServiceJava.startServer("127.0.0.1", boundPort, List.of(), BUNDLE_CL);
      assertTrue(second.isFailure(), "starting on an already-bound port must yield failure");
    } finally {
      occupier.stop();
      occupier.destroy();
    }
  }

  @Test
  void startsServerWithExplicitRoutesAndRegistersThem() throws Exception {
    Result<Server, Exception> result =
        CoreUIServiceJava.startServer("127.0.0.1", 0, List.of(DemoView.class), BUNDLE_CL);
    assertTrue(result.isSuccess());
    Server server = result.getOrThrow();
    try {
      WebAppContext webapp = (WebAppContext) server.getHandler();
      VaadinServletContext ctx = new VaadinServletContext(webapp.getServletContext());
      ApplicationRouteRegistry registry = ApplicationRouteRegistry.getInstance(ctx);
      var navigationTarget = RouteConfiguration.forRegistry(registry)
                                               .getRoute("")
                                               .orElseThrow();
      assertEquals(DemoView.class, navigationTarget,
                   "DemoView must be registered for the empty route");
    } finally {
      server.stop();
      server.destroy();
    }
  }

  @Test
  void scanForRoutesFindsAnnotatedComponents() {
    Set<Class<? extends Component>> routes = CoreUIServiceJava.scanForRoutes("demo");
    assertTrue(routes.contains(DemoView.class),
               () -> "scanForRoutes must include DemoView, got " + routes);
  }

  @Test
  void scanForRoutesIgnoresPackagesWithoutRoutes() {
    Set<Class<? extends Component>> routes = CoreUIServiceJava.scanForRoutes("java.lang");
    assertTrue(routes.isEmpty(),
               () -> "java.lang holds no @Route classes, got " + routes);
  }

  @Test
  void twoArgOverloadProbesViaDefaultClassLoaderAndFailsWithoutBundle() {
    // src/test/resources deliberately does not ship a bundle stub (so the demo
    // launched via `exec:java -Dexec.classpathScope=test` cannot mistake the stub
    // for the real bundle). Consequently the 2-arg overload — which probes via
    // the library's own ClassLoader — must surface Result.failure here.
    Result<Server, Exception> result = CoreUIServiceJava.startServer("127.0.0.1", 0);
    assertTrue(result.isFailure(), () -> "expected failure, got: " + result);
  }

  @Test
  void threeArgOverloadProbesViaDefaultClassLoaderAndFailsWithoutBundle() {
    Result<Server, Exception> result =
        CoreUIServiceJava.startServer("127.0.0.1", 0, List.of(DemoView.class));
    assertTrue(result.isFailure(), () -> "expected failure, got: " + result);
  }

  @Test
  void failsLoudlyWhenFrontendBundleIsMissing() {
    ClassLoader emptyClassLoader = new URLClassLoader("emptyForBundleProbe", new URL[0], null);
    Result<Server, Exception> result = CoreUIServiceJava.startServer(
        "127.0.0.1", 0, List.<Class<? extends Component>>of(), emptyClassLoader);
    assertTrue(result.isFailure(), () -> "expected failure, got: " + result);
    String message = result.fold(s -> "", err -> err.getMessage());
    assertTrue(message.contains("index.html"),
               () -> "error message should mention index.html, got: " + message);
    assertTrue(message.contains("build-frontend"),
               () -> "error message should point at build-frontend, got: " + message);
  }

  private static int findFreePort() throws Exception {
    try (var socket = new java.net.ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  // Drops a minimal META-INF/VAADIN/webapp/index.html into a temp dir and returns a
  // URLClassLoader rooted at it, parented to the system loader so Vaadin's own classes
  // still resolve. The 4-arg startServer probes this classloader for the bundle marker;
  // happy-path tests thus pass without polluting src/test/resources/.
  private static ClassLoader bundleStubClassLoader() {
    try {
      Path stubRoot = Files.createTempDirectory("nvj-bundle-stub");
      Path indexHtml = stubRoot.resolve("META-INF/VAADIN/webapp/index.html");
      Files.createDirectories(indexHtml.getParent());
      Files.writeString(indexHtml, "<!doctype html><title>nvj bundle stub</title>\n");
      return new URLClassLoader("nvj-bundle-stub",
                                new URL[] { stubRoot.toUri().toURL() },
                                CoreUIServiceJava.class.getClassLoader());
    } catch (IOException e) {
      throw new RuntimeException("failed to set up bundle stub classloader", e);
    }
  }
}
