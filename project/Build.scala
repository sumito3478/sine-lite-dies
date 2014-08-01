import sbt._
import Keys._
import Defaults._
import java.io._

object Build extends Build {
  import scalariform.formatter.preferences._
  import com.typesafe.sbt.SbtScalariform
  lazy val scalariformSettings = SbtScalariform.scalariformSettings ++ Seq(
    SbtScalariform.ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(DoubleIndentClassDeclaration, true))

  import de.johoop.jacoco4sbt._
  import JacocoPlugin._

  lazy val findbugsSettings = {
    import de.johoop.findbugs4sbt._
    import FindBugs._
    FindBugs.findbugsSettings ++ Seq(
      findbugsReportType := Some(ReportType.FancyHtml),
      findbugsReportPath := Some(crossTarget.value / "findbugs" / "report.html")
    )
  }

  import org.scalastyle.{ sbt => scalastyle }
  lazy val scalastyleSettings = scalastyle.ScalastylePlugin.Settings ++ Seq(
    scalastyle.PluginKeys.config := file("./res/test/scalastyle-config.xml")
  )

  lazy val releaseSettings = {
    import sbtrelease.ReleasePlugin
    import ReleasePlugin.ReleaseKeys._
    ReleasePlugin.releaseSettings ++ Seq(
      crossBuild := true,
      tagComment <<= (version in ThisBuild) map (v => s"Release $v"),
      commitMessage <<= (version in ThisBuild) map (v => s"Bump version number to $v"))
  }

  lazy val macroParadiseSettings = Seq(
    resolvers += Resolver.sonatypeRepo("release"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))

  lazy val commonSettings =
    scalariformSettings ++
      jacoco.settings ++
      findbugsSettings ++
      scalastyleSettings ++
      releaseSettings ++
      macroParadiseSettings ++
      Seq(
        scalaVersion := "2.11.2",
        scalacOptions ++= Seq(
          "-encoding", "utf-8",
          "-target:jvm-1.7",
          "-deprecation",
          "-feature",
          "-unchecked",
          "-Xexperimental",
          "-Xcheckinit",
          "-Xlint"),
        fork := true,
        scalaSource in Compile := baseDirectory.value / "src",
        scalaSource in Test := baseDirectory.value / "test",
        javaSource in Compile := baseDirectory.value / "src",
        javaSource in Test := baseDirectory.value / "test",
        resourceDirectory in Compile := baseDirectory.value / "res",
        resourceDirectory in Test := baseDirectory.value / "res-test",
        javaOptions := Seq("-Xms1024m"),
        organization := "info.sumito3478",
        licenses +=("Apache", url("http://www.apache.org/licenses/LICENSE-2.0")),
        incOptions := incOptions.value withNameHashing true,
        resolvers += "Sonatype public" at "https://oss.sonatype.org/content/groups/public/",
        libraryDependencies ++= Seq(
          "ch.qos.logback" % "logback-classic" % "1.1.2",
          "com.typesafe" % "config" % "1.2.1",
          "com.typesafe.akka" %% "akka-actor" % "2.3.4",
          "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
          "net.ceedubs" %% "ficus" % "1.1.1",
          "org.apache.commons" % "commons-lang3" % "3.3.2",
          "org.postgresql" % "postgresql" % "9.3-1101-jdbc41",
          "org.scalatest" %% "scalatest" % "2.2.0",
          "org.scalatra.scalate" %% "scalate-core" % "1.7.0",
          "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
          "org.slf4j" % "jul-to-slf4j" % "1.7.7",
          "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
          "org.yaml" % "snakeyaml" % "1.14-SNAPSHOT",
          "org.slf4j" % "slf4j-api" % "1.7.7"))

  lazy val `reflect-core` = project settings (commonSettings: _*)
  lazy val core = project settings (commonSettings: _*) dependsOn `reflect-core`

  lazy val `sine-lite-dies` = project in file(".") aggregate(`reflect-core`, core) settings (commonSettings: _*) settings (
    libraryDependencies ++= Seq(
      "net.databinder" %% "unfiltered-filter" % "0.8.0")) dependsOn(core)
}

