import Dependencies.*

ThisBuild / version      := sys.env.getOrElse("CI_SHORT_COMMIT_ID", "latest")
ThisBuild / scalaVersion := "2.13.13"

lazy val utg_backend =
  project
    .in(file("."))
    .settings(
      name := "utg_backend"
    )
    .aggregate(
      endpoints,
      integrations,
      supports,
      common,
    )

lazy val common =
  project
    .in(file("common"))
    .settings(
      name := "common",
      libraryDependencies ++=
        Dependencies.io.circe.all ++
          eu.timepit.refined.all ++
          com.github.pureconfig.all ++
          com.beachape.enumeratum.all ++
          tf.tofu.derevo.all ++
          Seq(
            uz.scala.common,
            org.typelevel.cats.core,
            org.typelevel.cats.effect,
            com.github.cb372.retry,
            com.github.tototoshi.scalaCsv,
            org.typelevel.log4cats,
            org.apache.poi,
            org.apache.ooxml,
            ch.qos.logback,
            dev.optics.monocle,
            Dependencies.io.scalaland.chimney,
            Dependencies.io.estatico.newtype,
            Dependencies.io.github.jmcardon.`tsec-password`,
            Dependencies.org.openpdf.core,
          ),
    )
    .dependsOn(LocalProject("support_logback"))

lazy val integrations = project
  .in(file("integrations"))
  .settings(
    name := "integrations"
  )

lazy val supports = project
  .in(file("supports"))
  .settings(
    name := "supports"
  )

lazy val endpoints = project
  .in(file("endpoints"))
  .settings(
    name := "endpoints"
  )

addCommandAlias(
  "styleCheck",
  "all scalafmtSbtCheck; scalafmtCheckAll; Test / compile; scalafixAll --check",
)

Global / lintUnusedKeysOnLoad := false
Global / onChangedBuildSource := ReloadOnSourceChanges
