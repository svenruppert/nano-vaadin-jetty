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

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.vaadin.nano.CoreUIServiceJava;
import org.eclipse.jetty.server.Server;

public final class DemoApplication {

  private static final String HOST = "127.0.0.1";
  private static final int PORT = 8080;

  private DemoApplication() {
  }

  static void main(String[] args) throws InterruptedException {
    HasLogger.staticLogger().info("Demo application starting on http://{}:{}/", HOST, PORT);
    Server server = CoreUIServiceJava
        .startServer(HOST, PORT, CoreUIServiceJava.scanForRoutes("demo"))
        .getOrThrow();
    server.join();
  }
}
