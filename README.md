# todomvc-playwright-scala
Scala implementation with Playwright Java API of [Todomvc Application Specification](https://github.com/tastejs/todomvc/blob/master/app-spec.md)
# Usage
Add to your build.sbt libraryDependencies with
```scala
"todomvc-playwright-scala" %% "todomvc-playwright-scala" % "1.0.0" % Test
```
Add a test class that inherits from `todomvc_playwright_scala.TodomvcSuite`:
```scala
import todomvc_playwright_scala.TodomvcSuite

class Test extends TodomvcSuite {}
```
The Todomvc app under test should be available at `localhost:3000`
