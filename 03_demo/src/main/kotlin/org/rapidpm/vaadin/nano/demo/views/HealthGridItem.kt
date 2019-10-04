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
