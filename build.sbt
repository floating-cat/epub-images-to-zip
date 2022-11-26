val scala3Version = "3.2.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "epub-images-to-zip",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "2.1.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )

ThisBuild / semanticdbEnabled := true
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
