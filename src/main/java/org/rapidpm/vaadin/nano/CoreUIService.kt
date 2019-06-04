/**
 * Copyright © 2017 Sven Ruppert (sven.ruppert@gmail.com)
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rapidpm.vaadin.nano

import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.*
import org.rapidpm.dependencies.core.logger.HasLogger
import org.rapidpm.frp.model.Result

import java.lang.Integer.valueOf
import java.lang.System.getProperty
import org.rapidpm.frp.model.Result.failure

/**
 *
 */
class CoreUIService : HasLogger {

  var jetty = failure<Server>("not initialised so far")

  fun startup() {

    try {
      val context = WebAppContext()
      context.isLogUrlOnStart = true
      context.isConfigurationDiscovered = true
      context.configurations = arrayOf<Configuration>(AnnotationConfiguration(), WebInfConfiguration(), WebXmlConfiguration(), MetaInfConfiguration())

      context.contextPath = "/"
      val classPathResource = Resource.newClassPathResource("/META-INF/resources", true, true)
      context.baseResource = classPathResource
      context.allowNullPathInfo = true
      context.setAttribute(JAR_PATTERN, ".*")

      val server = Server(valueOf(getProperty(CORE_UI_SERVER_PORT, CORE_UI_SERVER_PORT_DEFAULT)))
      server.handler = context

      server.start()
      server.join()
      jetty = Result.success(server)
    } catch (e: Exception) {
      logger().warning(e.localizedMessage)
    }

  }

  companion object {

    val JAR_PATTERN = "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern"


    val CORE_UI_SERVER_HOST_DEFAULT = "0.0.0.0"
    val CORE_UI_SERVER_PORT_DEFAULT = "8899"

    val CORE_UI_SERVER_HOST = "core-ui-server-host"
    val CORE_UI_SERVER_PORT = "core-ui-server-port"
  }
}
