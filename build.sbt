ThisBuild / version := "1.0.0"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "todomvc-playwright-scala",
    libraryDependencies ++= Seq(
      "com.microsoft.playwright" % "playwright" % "1.42.0",
      "org.scalatest" %% "scalatest" % "3.2.18",
      "com.lihaoyi" %% "upickle" % "3.2.0"
    )
  )
