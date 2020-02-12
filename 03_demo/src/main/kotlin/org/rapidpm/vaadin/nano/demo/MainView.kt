/**
 * Copyright © 2013 Sven Ruppert (sven.ruppert@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rapidpm.vaadin.nano.demo

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.TabVariant
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.PWA
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import org.rapidpm.vaadin.nano.demo.views.DashboardView
import java.util.*

/**
 * The main view is a top-level placeholder for other views.
 */
@JsModule("./styles/shared-styles.js")
@PWA(name = "Nano-Vaadin for Flow", shortName = "Nano-Vaadin for Flow")
@Theme(value = Lumo::class, variant = Lumo.LIGHT)
class MainView : AppLayout() {

  private val menu: Tabs

  private val availableTabs: Array<Tab>
    get() {
      val tabs = ArrayList<Tab>()
      tabs.add(createTab("Dashboard", DashboardView::class.java))
      return tabs.toTypedArray()
    }

  init {
    menu = createMenuTabs()
    addToNavbar(menu)
  }

  private fun createMenuTabs(): Tabs {
    val tabs = Tabs()
    tabs.orientation = Tabs.Orientation.HORIZONTAL
    tabs.add(*availableTabs)
    return tabs
  }

  private fun createTab(title: String,
                        viewClass: Class<out Component>): Tab {
    return createTab(populateLink(RouterLink(null, viewClass), title))
  }

  private fun createTab(content: Component): Tab {
    val tab = Tab()
    tab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP)
    tab.add(content)
    return tab
  }

  private fun <T : HasComponents> populateLink(a: T, title: String): T {
    a.add(title)
    return a
  }

}
