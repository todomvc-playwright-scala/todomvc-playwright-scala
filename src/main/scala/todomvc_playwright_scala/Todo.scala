package todomvc_playwright_scala

import upickle.default.ReadWriter
case class Todo(title: String, completed: Boolean) derives ReadWriter
