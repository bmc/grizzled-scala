// ---------------------------------------------------------------------------
// Basic settings

name := "grizzled-scala"

version := "1.1.5"

organization := "org.clapper"

licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))

homepage := Some(url("http://software.clapper.org/grizzled-scala/"))

description := "A general-purpose Scala utility library"

scalaVersion := "2.10.3"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.3")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-P:continuations:enable")

seq(lsSettings :_*)

LsKeys.tags in LsKeys.lsync := Seq("utility", "library", "grizzled")

description in LsKeys.lsync <<= description(d => d)

crossScalaVersions := Seq(
  "2.10.0"
)


// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) =>
  // ScalaTest still uses the (deprecated) scala.actors API.
  deps :+ "org.scala-lang" % "scala-actors" % sv % "test"
}

// ---------------------------------------------------------------------------
// Other dependendencies

libraryDependencies += "jline" % "jline" % "2.6"

// ---------------------------------------------------------------------------
// Publishing criteria

publishTo := Some(Opts.resolver.sonatypeStaging)

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
