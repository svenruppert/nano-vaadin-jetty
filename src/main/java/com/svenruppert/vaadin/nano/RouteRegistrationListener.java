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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.Collection;
import java.util.List;

// Registers @Route classes against Vaadin's ApplicationRouteRegistry during ServletContext init —
// runs before any request can be served, which avoids the race of registering after server.start().
// Needed because embedded Jetty's annotation scanner only walks JARs and WEB-INF/classes, so
// @Route classes living in plain classpath directories never reach Vaadin's RouteRegistryInitializer.
final class RouteRegistrationListener implements ServletContextListener {

  private final List<Class<? extends Component>> routes;

  RouteRegistrationListener(Collection<Class<? extends Component>> routes) {
    this.routes = List.copyOf(routes);
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    VaadinServletContext ctx = new VaadinServletContext(sce.getServletContext());
    ApplicationRouteRegistry registry = ApplicationRouteRegistry.getInstance(ctx);
    RouteConfiguration config = RouteConfiguration.forRegistry(registry);
    routes.forEach(config::setAnnotatedRoute);
  }
}
