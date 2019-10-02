/**
 * Copyright © 2017 Sven Ruppert (sven.ruppert@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rapidpm.vaadin.nano;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.*;
import org.rapidpm.dependencies.core.logger.HasLogger;
import org.rapidpm.frp.model.Result;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static org.rapidpm.frp.model.Result.failure;

/**
 *
 */
public class CoreUIServiceJava
    implements HasLogger {

  public static final String JAR_PATTERN = "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern";


  public static final String CORE_UI_SERVER_HOST_DEFAULT = "0.0.0.0";
  public static final String CORE_UI_SERVER_PORT_DEFAULT = "8899";

  public static final String CORE_UI_SERVER_HOST = "core-ui-server-host";
  public static final String CORE_UI_SERVER_PORT = "core-ui-server-port";

  public Result<Server> jetty = failure("not initialised so far");


  public static final String CLI_HOST = "host";
  public static final String CLI_PORT = "port";

  public static void main(String[] args) throws ParseException {
    new CoreUIServiceJava().executeCLIOLDY(args).startup();
  }

  public CoreUIServiceJava executeCLIOLDY(String[] args) throws ParseException {
    final Options options = new Options();
    options.addOption(CLI_HOST, true, "host to use");
    options.addOption(CLI_PORT, true, "port to use");

    DefaultParser parser = new DefaultParser();
    CommandLine   cmd    = parser.parse(options, args);

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
      WebAppContext context = new WebAppContext();
      context.setLogUrlOnStart(true);
      context.setConfigurationDiscovered(true);
      context.setConfigurations(new Configuration[]{
          new AnnotationConfiguration(),
          new WebInfConfiguration(),
          new WebXmlConfiguration(),
          new MetaInfConfiguration()
      });

      context.setContextPath("/");
      Resource classPathResource = Resource.newClassPathResource("/META-INF/resources", true, true);
      context.setBaseResource(classPathResource);
      context.setAllowNullPathInfo(true);
      context.setAttribute(JAR_PATTERN , ".*");

      Server server = new Server(Integer.parseInt(getProperty(CORE_UI_SERVER_PORT, CORE_UI_SERVER_PORT_DEFAULT)));
      server.setHandler(context);

      server.start();
      server.join();
      jetty = Result.success(server);
    } catch (Exception e) {
      logger().warning(e.getLocalizedMessage());
    }
  }
}
