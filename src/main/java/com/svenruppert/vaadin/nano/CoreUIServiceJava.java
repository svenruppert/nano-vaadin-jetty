package com.svenruppert.vaadin.nano;

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

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.functional.result.Result;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CoreUIServiceJava {

  private CoreUIServiceJava() {
  }

  public static Result<Server, Exception> startServer(String host, int port) {
    return startServer(host, port, List.of());
  }

  // Starts the Jetty+Vaadin server and registers the given @Route classes against Vaadin's
  // ApplicationRouteRegistry before the first request can be served. Embedded Jetty does not
  // discover @Route classes that live in plain classpath directories (target/classes,
  // target/test-classes), so consumers must hand them in explicitly — either as a known list or
  // via {@link #scanForRoutes(String...)}.
  public static Result<Server, Exception> startServer(String host, int port,
                                                      Collection<Class<? extends Component>> routes) {
    try {
      Server server = new Server();
      HttpConfiguration httpConfig = new HttpConfiguration();
      ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
      connector.setHost(host);
      connector.setPort(port);
      server.addConnector(connector);

      WebAppContext webapp = new WebAppContext();

      ResourceFactory rf = ResourceFactory.of(webapp);
      Resource base = rf.newClassLoaderResource("META-INF/resources", true);
      if (base == null) {
        base = rf.newMemoryResource(CoreUIServiceJava.class.getResource("/"));
      }
      webapp.setBaseResource(base);

      webapp.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN, ".*\\.jar$");
      webapp.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*\\.jar$");
      webapp.setParentLoaderPriority(true);

      ServletHolder holder = new ServletHolder(new VaadinServlet());
      holder.setInitOrder(1);
      holder.setInitParameter("productionMode", "true");
      webapp.addServlet(holder, "/*");

      if (!routes.isEmpty()) {
        webapp.addEventListener(new RouteRegistrationListener(routes));
      }

      server.setHandler(webapp);
      server.start();
      HasLogger.staticLogger().info("Listening on http://{}:{}/", host, port);
      return Result.success(server);
    } catch (Exception e) {
      return Result.failure(e);
    }
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
          .map(info -> info.loadClass(false))
          .filter(Component.class::isAssignableFrom)
          .map(c -> c.asSubclass(Component.class))
          .collect(Collectors.toUnmodifiableSet());
    }
  }
}