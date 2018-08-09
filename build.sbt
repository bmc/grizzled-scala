// ---------------------------------------------------------------------------
// Basic settings

name := "grizzled-scala"
version := "4.5.1"
organization := "org.clapper"
licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))
homepage := Some(url("http://software.clapper.org/grizzled-scala/"))
description := "A general-purpose Scala utility library"
scalaVersion := "2.11.12"
crossScalaVersions := Seq("2.11.12", "2.12.6", "2.13.0-M4")

Seq(Compile, Test).map { scope =>
  unmanagedSourceDirectories in scope += {
    val base = baseDirectory.value / "src" / Defaults.nameForSrc(scope.name)
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v >= 13 =>
        base / s"scala-2.13+"
      case _ =>
        base / s"scala-2.13-"
    }
  }
}

// TODO remove this setting if Scala 2.13.0-M5 released
// https://github.com/scala/scala/commit/7c68757845d8adca4cee7fac9bd98de82c593ba8#diff-a7e7b847970727f538a3c4f0416d36b8R26
testOptions in Test in ThisBuild ++= {
  if (scalaVersion.value == "2.13.0-M4") {
    Seq(Tests.Exclude(Set(
      "grizzled.io.SourceReaderSpec"
    )))
  } else {
    Nil
  }
}

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

autoCompilerPlugins := true
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
bintrayPackageLabels := Seq("library", "grizzled", "scala")

wartremoverErrors in (Compile, compile) ++= Seq(
  Wart.ArrayEquals,
  // Wart.Any,
  Wart.AnyVal,
  Wart.AsInstanceOf,
  Wart.EitherProjectionPartial,
  Wart.Enumeration,
  Wart.ExplicitImplicitTypes,
  Wart.FinalCaseClass,
  Wart.FinalVal,
  Wart.IsInstanceOf,
  Wart.JavaConversions,
  Wart.LeakingSealed,
  Wart.MutableDataStructures,
  // Wart.NonUnitStatements,
  // Wart.Nothing,
  Wart.Null,
  Wart.Option2Iterable,
  Wart.OptionPartial,
  Wart.PublicInference,
  Wart.Return,
  Wart.StringPlusAny,
  Wart.Throw,
  Wart.TraversableOps,
  Wart.TryPartial,
  Wart.Var,
  Wart.While
)


// ---------------------------------------------------------------------------
// Helpers

// ---------------------------------------------------------------------------
// ScalaTest

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-collection-compat" % "0.1.1",
  "org.scalatest" %% "scalatest" % "3.0.6-SNAP1" % "test"
)

parallelExecution in Test := true

// ---------------------------------------------------------------------------
// Other tasks

// ---------------------------------------------------------------------------
// Publishing criteria

// Don't set publishTo. The Bintray plugin does that automatically.

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
pomExtra :=
  <scm>
    <url>git@github.com:bmc/grizzled-scala.git/</url>
    <connection>scm:git:git@github.com:bmc/grizzled-scala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>bmc</id>
      <name>Brian Clapper</name>
      <url>http://www.clapper.org/bmc</url>
    </developer>
  </developers>

