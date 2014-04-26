// ---------------------------------------------------------------------------
// Basic settings

name := "grizzled-scala"

version := "1.2"

organization := "org.clapper"

licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))

homepage := Some(url("http://software.clapper.org/grizzled-scala/"))

description := "A general-purpose Scala utility library"

scalaVersion := "2.11.0"

crossScalaVersions := Seq("2.10.4", "2.11.0")

// Incremental compilation performance improvement. See
// http://scala-lang.org/news/2014/04/21/release-notes-2.11.0.html

incOptions := incOptions.value.withNameHashing(true)

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

autoCompilerPlugins := true

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

seq(lsSettings :_*)

LsKeys.tags in LsKeys.lsync := Seq("utility", "library", "grizzled")

description in LsKeys.lsync <<= description(d => d)

seq(bintraySettings: _*)

// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.scala-lang.modules" %% "scala-async" % "0.9.1"
)

libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) =>
  // ScalaTest still uses the (deprecated) scala.actors API.
  deps :+ "org.scala-lang" % "scala-actors" % sv % "test"
}

// ---------------------------------------------------------------------------
// Other dependendencies

libraryDependencies += "jline" % "jline" % "2.6"

// ---------------------------------------------------------------------------
// Publishing criteria

// Don't set publishTo. The Bintray plugin does that automatically.

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
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
)
