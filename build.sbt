// ---------------------------------------------------------------------------
// Basic settings

name := "grizzled-scala"

version := "1.0.14"

organization := "org.clapper"

licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))

homepage := Some(url("http://software.clapper.org/grizzled-scala/"))

description := "A general-purpose Scala utility library"

scalaVersion := "2.10.0-M7"

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

autoCompilerPlugins := true

//seq(lsSettings :_*)

//(LsKeys.tags in LsKeys.lsync) := Seq("utility", "library", "grizzled")

//(description in LsKeys.lsync) <<= description(d => d)

crossScalaVersions := Seq(
  "2.10.0-M7"
)

// ---------------------------------------------------------------------------
// ScalaTest dependendency

libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) =>
    // Select ScalaTest version based on Scala version
    val scalatestVersionMap = Map(
      "2.8.0"     -> ("scalatest_2.8.0", "1.3.1.RC2"),
      "2.8.1"     -> ("scalatest_2.8.1", "1.7.1"),
      "2.8.2"     -> ("scalatest_2.8.2", "1.7.1"),
      "2.9.0"     -> ("scalatest_2.9.0", "1.7.1"),
      "2.9.0-1"   -> ("scalatest_2.9.0-1", "1.7.1"),
      "2.9.1"     -> ("scalatest_2.9.1", "1.7.1"),
      "2.9.1-1"   -> ("scalatest_2.9.1", "1.7.1"),
      "2.9.2"     -> ("scalatest_2.9.1", "1.7.1"),
      "2.10.0-M7" -> ("scalatest_2.10.0-M7", "1.9-2.10.0-M7-B1")
    )
    val (scalatestArtifact, scalatestVersion) = scalatestVersionMap.getOrElse(
        sv, error("Unsupported Scala version for ScalaTest: " + scalaVersion)
    )
    deps :+ "org.scalatest" % scalatestArtifact % scalatestVersion % "test"
}

// ---------------------------------------------------------------------------
// Other dependendencies

libraryDependencies += "jline" % "jline" % "2.6"

// ---------------------------------------------------------------------------
// Publishing criteria

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

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
