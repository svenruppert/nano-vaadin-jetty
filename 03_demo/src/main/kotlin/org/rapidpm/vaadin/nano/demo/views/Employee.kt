package org.rapidpm.vaadin.nano.demo.views

class Employee {

  var firstname: String? = null
  var lastname: String? = null
  var title: String? = null
  var email: String? = null
  var notes: String? = null

  constructor(firstname: String, lastname: String, email: String,
              title: String) : super() {
    this.firstname = firstname
    this.lastname = lastname
    this.email = email
    this.title = title
  }

  constructor() {

  }

  override fun toString(): String {
    return "$firstname $lastname($email)"
  }
}
