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
package org.rapidpm.vaadin.nano.demo.views

import com.github.appreciated.css.grid.GridLayoutComponent.AutoFlow.ROW_DENSE
import com.github.appreciated.css.grid.GridLayoutComponent.Overflow.AUTO
import com.github.appreciated.css.grid.sizes.Flex
import com.github.appreciated.css.grid.sizes.Length
import com.github.appreciated.css.grid.sizes.MinMax
import com.github.appreciated.css.grid.sizes.Repeat.RepeatMode.AUTO_FIT
import com.github.appreciated.layout.FlexibleGridLayout
import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteAlias
import org.rapidpm.dependencies.core.logger.HasLogger
import org.rapidpm.vaadin.nano.demo.MainView
import org.rapidpm.vaadin.nano.demo.views.cards.ButtonClickCard
import org.rapidpm.vaadin.nano.demo.views.cards.ServiceHealtCard

@Route(value = "dashboard", layout = MainView::class)
@RouteAlias(value = "", layout = MainView::class)
@PageTitle("Dashboard")
@CssImport(value = "styles/views/dashboard/dashboard-view.css", include = "lumo-badge")
@JsModule("@vaadin/vaadin-lumo-styles/badge.js")
class DashboardView : Composite<Div>(), HasLogger {


  private val gridLayout = FlexibleGridLayout()
      .withColumns(AUTO_FIT,
          MinMax(Length("300px"), Flex(1.0)))
//      .withAutoRows(Length("190px"))
      .withPadding(true)
      .withSpacing(true)
      .withAutoFlow(ROW_DENSE)
      .withOverflow(AUTO)

  init {
    setId("dashboard-view")

    content.add(gridLayout)
    gridLayout.withItems(
        ServiceHealtCard(),
        ButtonClickCard(),
        ServiceHealtCard())
    gridLayout.withItemWithSize(ServiceHealtCard(), 2,1)
    gridLayout.withItems(
        ServiceHealtCard(),
        ServiceHealtCard())
  }

}
