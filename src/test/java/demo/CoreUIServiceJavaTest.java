package demo;

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

import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import com.svenruppert.vaadin.nano.CoreUIServiceJava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreUIServiceJavaTest {

  @Test
  void startsJettyOnEphemeralPortWithVaadinServlet() throws Exception {
    Server server = CoreUIServiceJava.startServer("127.0.0.1", 0);
    try {
      assertTrue(server.isStarted());

      Connector connector = server.getConnectors()[0];
      ServerConnector serverConnector = assertInstanceOf(ServerConnector.class, connector);
      assertTrue(serverConnector.getLocalPort() > 0);
      assertEquals("127.0.0.1", serverConnector.getHost());

      WebAppContext webAppContext = assertInstanceOf(WebAppContext.class, server.getHandler());
      assertEquals("/", webAppContext.getContextPath());

      ServletHolder holder = webAppContext.getServletHandler().getServlets()[0];
      assertEquals("true", holder.getInitParameter("productionMode"));
      assertTrue(holder.isAsyncSupported());
    } finally {
      server.stop();
      server.destroy();
    }

    assertFalse(server.isRunning());
  }
}
