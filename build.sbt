// ---------------------------------------------------------------------------
// Basic settings

name := "grizzled-scala"
version := "4.7.1"
organization := "org.clapper"
licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))
homepage := Some(url("http://software.clapper.org/grizzled-scala/"))
description := "A general-purpose Scala utility library"
scalaVersion := "2.12.7"
crossScalaVersions := Seq("2.11.12", "2.12.7", "2.13.0-M5")

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
// Dependencies

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-collection-compat" % "0.2.1",
  "org.scalatest"          %% "scalatest"               % "3.0.6-SNAP5" % Test
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

