// ---------------------------------------------------------------------------
// Basic settings

name := "grizzled-scala"
version := "4.5.0"
organization := "org.clapper"
licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))
homepage := Some(url("http://software.clapper.org/grizzled-scala/"))
description := "A general-purpose Scala utility library"
scalaVersion := "2.11.12"
crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.6")

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

autoCompilerPlugins := true
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
bintrayPackageLabels := Seq("library", "grizzled", "scala")

// Wart Remover doesn't yet work with 2.12. Disabled for now.
wartremoverErrors in (Compile, compile) ++= Seq(
  Wart.ArrayEquals,
  // Wart.Any,
  // Wart.AnyVal,
  Wart.AsInstanceOf,
  Wart.EitherProjectionPartial,
  Wart.Enumeration,
  Wart.ExplicitImplicitTypes,
  Wart.FinalCaseClass,
//  Wart.FinalVal,
//  Wart.IsInstanceOf,
//  Wart.JavaConversions,
//  Wart.LeakingSealed,
//  Wart.MutableDataStructures,
//  Wart.NonUnitStatements,
//  Wart.Nothing,
//  Wart.Null,
//  Wart.Option2Iterable,
//  Wart.OptionPartial,
//  Wart.PublicInference,
//  Wart.Return,
//  Wart.StringPlusAny,
//  Wart.Throw,
//  Wart.TraversableOps,
//  Wart.TryPartial,
//  Wart.Var,
  Wart.While
)


// ---------------------------------------------------------------------------
// Helpers

// ---------------------------------------------------------------------------
// ScalaTest

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
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

