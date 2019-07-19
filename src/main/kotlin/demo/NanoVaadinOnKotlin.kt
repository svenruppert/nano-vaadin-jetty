package demo

import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.rapidpm.dependencies.core.logger.HasLogger
import org.rapidpm.vaadin.nano.CoreUIService
import org.stagemonitor.core.Stagemonitor
import java.lang.System.setProperty


fun main(args: Array<String>) {

  val options = Options()
  options.addOption(CoreUIService.CLI_HOST, true, "host to use")
  options.addOption(CoreUIService.CLI_PORT, true, "port to use")

  val parser = DefaultParser()
  val cmd = parser.parse(options, args)

  if (cmd.hasOption(CoreUIService.CLI_HOST)) {
    setProperty(CoreUIService.CORE_UI_SERVER_HOST, cmd.getOptionValue(CoreUIService.CLI_HOST))
  }
  if (cmd.hasOption(CoreUIService.CLI_PORT)) {
    setProperty(CoreUIService.CORE_UI_SERVER_PORT, cmd.getOptionValue(CoreUIService.CLI_PORT))
  }

  Stagemonitor.init();

  CoreUIService().startup()
}



@Route("")
class VaadinApp : Composite<Div>(), HasLogger {

  private val btnClickMe = Button("click me")
  private val lbClickCount = Span("0")
  private val layout = VerticalLayout(btnClickMe, lbClickCount)

  private var clickcount = 0

  init {
    btnClickMe.setId(BTN_CLICK_ME)
    btnClickMe.addClickListener { event -> lbClickCount.text = (++clickcount).toString() }

    lbClickCount.setId(LB_CLICK_COUNT)

    logger().info("and now..  setting the main content.. ")
    content.add(layout)
  }

  companion object {

    val BTN_CLICK_ME = "btn-click-me"
    val LB_CLICK_COUNT = "lb-click-count"
  }
}