package demo

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.rapidpm.vaadin.nano.CoreUIServiceJava
import org.rapidpm.vaadin.nano.CoreUIServiceKotlin
import org.stagemonitor.core.Stagemonitor
import java.lang.System.setProperty

object StartupKotlin {

  val CLI_HOST = "host"
  val CLI_PORT = "port"

  @Throws(ParseException::class)
  @JvmStatic
  fun main(args: Array<String>) {
    //init i18n
    //init stagemonitor
    val options = Options()
    options.addOption(CLI_HOST, true, "host to use")
    options.addOption(CLI_PORT, true, "port to use")

    val parser = DefaultParser()
    val cmd = parser.parse(options, args)

    if (cmd.hasOption(CLI_HOST)) {
      setProperty(CoreUIServiceKotlin.CORE_UI_SERVER_HOST, cmd.getOptionValue(CLI_HOST))
    }
    if (cmd.hasOption(CLI_PORT)) {
      setProperty(CoreUIServiceKotlin.CORE_UI_SERVER_PORT, cmd.getOptionValue(CLI_PORT))
    }

    Stagemonitor.init()
    CoreUIServiceJava().startup()
  }
}