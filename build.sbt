// ---------------------------------------------------------------------------
// Basic settings

name := "grizzled-scala"
version := "2.4.1"
organization := "org.clapper"
licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))
homepage := Some(url("http://software.clapper.org/grizzled-scala/"))
description := "A general-purpose Scala utility library"
scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-M4")

// Incremental compilation performance improvement. See
// http://scala-lang.org/news/2014/04/21/release-notes-2.11.0.html

incOptions := incOptions.value.withNameHashing(true)

ivyScala := ivyScala.value.map { _.copy(overrideScalaVersion = true) }

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

autoCompilerPlugins := true
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
bintrayPackageLabels := Seq("library", "grizzled", "scala")

/* Wart Remover doesn't yet work with 2.12. Disabled for now.
wartremoverErrors in (Compile, compile) ++= Seq(
  Wart.Any2StringAdd,
  Wart.AsInstanceOf,
  Wart.EitherProjectionPartial,
  Wart.Enumeration,
  Wart.FinalCaseClass,
  Wart.IsInstanceOf,
  Wart.JavaConversions,
  Wart.Null,
  Wart.OptionPartial,
  Wart.Return,
  Wart.Var
)
*/

// ---------------------------------------------------------------------------
// Helpers

// Take a dependency and map its cross-compiled version, creating a new
// dependency. Temporary, until Scala 2.12 is for real.

def mappedDep(dep: sbt.ModuleID): sbt.ModuleID = {
  dep cross CrossVersion.binaryMapped {
    case v if v startsWith "2.12" => "2.11"
    case v => v.split("""\.""").take(2).mkString(".")
  }
}


// ---------------------------------------------------------------------------
// ScalaTest

libraryDependencies ++= Seq(
  mappedDep("org.scalatest" %% "scalatest" % "3.0.0-RC1" % "test")
)

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

