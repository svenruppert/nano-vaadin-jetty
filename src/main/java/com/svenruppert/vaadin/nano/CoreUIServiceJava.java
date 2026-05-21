package com.svenruppert.vaadin.nano;

/*-
 * #%L
 * nano-vaadin-jetty
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 - 2026 Vaadin
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.functional.model.Result;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.ee11.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.svenruppert.functional.model.Result.failure;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

/**
 *
 */
public class CoreUIServiceJava
    implements HasLogger {

  public static final String CORE_UI_SERVER_HOST_DEFAULT = "0.0.0.0";
  public static final String CORE_UI_SERVER_PORT_DEFAULT = "8899";

  public static final String CORE_UI_SERVER_HOST = "core-ui-server-host";
  public static final String CORE_UI_SERVER_PORT = "core-ui-server-port";
  public static final String CLI_HOST = "host";
  public static final String CLI_PORT = "port";
  private Result<Server> jetty = failure("not initialised so far");

  static void main(String[] args)
      throws ParseException {
    new CoreUIServiceJava().executeCLI(args).startup();
  }

  public static Server startServer(String host, int port)
      throws Exception {
    return startServer(host, port, List.of());
  }

  // Starts the Jetty+Vaadin server and registers the given @Route classes against Vaadin's
  // ApplicationRouteRegistry before the first request can be served. Embedded Jetty does not
  // discover @Route classes that live in plain classpath directories (target/classes,
  // target/test-classes), so consumers must hand them in explicitly — either as a known list or
  // via {@link #scanForRoutes(String...)}.
  public static Server startServer(String host, int port,
                                   Collection<Class<? extends Component>> routes)
      throws Exception {
    ensureProductionTokenFile();

    Server server = new Server();
    HttpConfiguration httpConfig = new HttpConfiguration();
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    connector.setHost(host);
    connector.setPort(port);
    server.addConnector(connector);

    WebAppContext webapp = new WebAppContext();
    webapp.setContextPath("/");
    webapp.setLogUrlOnStart(true);

    ResourceFactory rf = ResourceFactory.of(webapp);
    Resource base = rf.newClassLoaderResource("META-INF/resources", true);
    if (base == null) {
      base = rf.newMemoryResource(CoreUIServiceJava.class.getResource("/"));
    }
    webapp.setBaseResource(base);

    webapp.setConfigurationDiscovered(true);
    webapp.addConfiguration(new AnnotationConfiguration());
    webapp.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN, ".*\\.jar$");
    webapp.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*\\.jar$");
    webapp.setParentLoaderPriority(true);

    ServletHolder holder = new ServletHolder(new VaadinServlet());
    holder.setInitOrder(1);
    holder.setAsyncSupported(true);
    holder.setInitParameter("productionMode", "true");
    webapp.addServlet(holder, "/*");

    if (!routes.isEmpty()) {
      webapp.addEventListener(new RouteRegistrationListener(routes));
    }

    server.setHandler(webapp);
    server.start();
    return server;
  }

  // Scans the given packages on the runtime classpath for classes annotated with @Route that
  // extend Component, e.g. {@code scanForRoutes("com.example.views")}. Multiple packages may
  // be passed. Returned set is unmodifiable.
  public static Set<Class<? extends Component>> scanForRoutes(String... packages) {
    try (ScanResult scan = new ClassGraph()
        .acceptPackages(packages)
        .enableClassInfo()
        .enableAnnotationInfo()
        .scan()) {
      return scan.getClassesWithAnnotation(Route.class.getName())
          .stream()
          .map(info -> info.loadClass(true))
          .filter(Component.class::isAssignableFrom)
          .map(c -> c.asSubclass(Component.class))
          .collect(Collectors.toUnmodifiableSet());
    }
  }

  static void ensureProductionTokenFile()
      throws IOException {
    if (CoreUIServiceJava.class.getClassLoader()
        .getResource("META-INF/VAADIN/config/flow-build-info.json") != null) {
      return;
    }
    String url = CoreUIServiceJava.class.getProtectionDomain()
        .getCodeSource().getLocation().toString();
    if (!url.startsWith("file:") || !url.endsWith("/")) {
      return;
    }
    Path classesDir = Path.of(URI.create(url));
    Path target = classesDir.resolve("META-INF/VAADIN/config/flow-build-info.json");
    Path parent = target.getParent();
    if (parent == null) {
      return;
    }
    Files.createDirectories(parent);
    Files.writeString(target,
                      "{\"productionMode\":true,\"eagerServerLoad\":false,\"react.enable\":true}\n");
  }

  public Result<Server> jetty() {
    return jetty;
  }

  public CoreUIServiceJava executeCLI(String[] args)
      throws ParseException {
    final Options options = new Options();
    options.addOption(CLI_HOST, true, "host to use");
    options.addOption(CLI_PORT, true, "port to use");

    DefaultParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    if (cmd.hasOption(CLI_HOST)) {
      setProperty(CoreUIServiceJava.CORE_UI_SERVER_HOST, cmd.getOptionValue(CLI_HOST));
    }
    if (cmd.hasOption(CLI_PORT)) {
      setProperty(CoreUIServiceJava.CORE_UI_SERVER_PORT, cmd.getOptionValue(CLI_PORT));
    }
    return this;
  }

  public void startup() {
    try {
      int port = Integer.parseInt(getProperty(CORE_UI_SERVER_PORT, CORE_UI_SERVER_PORT_DEFAULT));
      String host = getProperty(CORE_UI_SERVER_HOST, CORE_UI_SERVER_HOST_DEFAULT);
      Server server = startServer(host, port);
      jetty = Result.success(server);
      server.join();
    } catch (Exception e) {
      logger().warn(e.getLocalizedMessage(), e);
    }
  }
}