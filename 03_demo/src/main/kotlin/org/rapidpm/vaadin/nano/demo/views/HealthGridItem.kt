package org.rapidpm.vaadin.nano.demo.views

import java.time.LocalDate

/**
 * Simple DTO class for the inbox list to demonstrate complex object data
 */
class HealthGridItem {

  var itemDate: LocalDate? = null
  var city: String? = null
  var country: String? = null
  var status: String? = null
  var theme: String? = null

  constructor() {
  }

  constructor(itemDate: LocalDate, city: String, country: String, status: String, theme: String) {
    this.itemDate = itemDate
    this.city = city
    this.country = country
    this.status = status
    this.theme = theme
  }
}
