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
package org.rapidpm.vaadin.nano.demo.views.cards

import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.renderer.ComponentRenderer
import org.rapidpm.dependencies.core.logger.HasLogger
import org.rapidpm.vaadin.nano.demo.views.HealthGridItem
import java.time.LocalDate
import java.util.*

class ServiceHealtCard : Composite<Div>(), HasLogger {
  private val grid = Grid<HealthGridItem>()
  private val label = H3("Service health")

  init {
    content.add(WrapperCard("wrapper", arrayOf(label, grid), CLASSNME))
    configGrid()
    initData()
  }

  private fun configGrid() {
    grid.addColumn({ it.city })
        .setHeader("City")
    grid.addColumn(ComponentRenderer<Span, HealthGridItem> { item ->
      val span = Span(item.status)
      span.element
          .themeList
          .add(item.theme)
      span
    })
        .setFlexGrow(0)
        .setWidth("100px")
        .setHeader("Status")
    grid.addColumn({ it.itemDate })
        .setHeader("Date")
        .setWidth("140px")
    grid.addThemeVariants(GridVariant.LUMO_NO_BORDER)
  }

  private fun initData() {
    // Grid
    val gridItems = ArrayList<HealthGridItem>()
    gridItems.add(HealthGridItem(LocalDate.of(2019, 1, 14), "M\u00FCnster", "Germany", "Good", "badge"))
    gridItems.add(HealthGridItem(LocalDate.of(2019, 1, 14), "Cluj-Napoca", "Romania", "Good", "badge"))
    gridItems.add(HealthGridItem(LocalDate.of(2019, 1, 14), "Ciudad Victoria", "Mexico", "Good", "badge"))
    gridItems.add(HealthGridItem(LocalDate.of(2019, 1, 14), "Ebetsu", "Japan", "Excellent", "badge success"))
    gridItems.add(
        HealthGridItem(LocalDate.of(2019, 1, 14), "S\u00E3o Bernardo do Campo", "Brazil", "Good", "badge"))
    gridItems.add(HealthGridItem(LocalDate.of(2019, 1, 14), "Maputo", "Mozambique", "Good", "badge"))
    gridItems.add(HealthGridItem(LocalDate.of(2019, 1, 14), "Warsaw", "Poland", "Good", "badge"))
    gridItems.add(HealthGridItem(LocalDate.of(2019, 1, 14), "Kasugai", "Japan", "Failing", "badge error"))
    gridItems.add(
        HealthGridItem(LocalDate.of(2019, 1, 14), "Lancaster", "United States", "Excellent", "badge success"))

    grid.setItems(gridItems)
  }

  companion object {
    val CLASSNME = "card"
  }
}
