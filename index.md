---
title: The Grizzled Scala Utility Library
layout: withTOC
---

[![Build Status](https://travis-ci.org/bmc/grizzled-scala.svg?branch=master)](https://travis-ci.org/bmc/grizzled-scala)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.clapper/grizzled-scala_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.clapper/grizzled-scala_2.11)

## Introduction

The Grizzled Scala Library is a general-purpose [Scala][] library with a
variety of different modules and packages. It's similar to the clapper.org
[Grizzled Python][] Library, only for Scala. (Duh.) It's roughly organized
into subpackages that group different kinds of utility functions and
classes. Currently, the library is broken into the following modules:

Currently, the library is broken into a number of modules:

* `grizzled.binary`: Some code that's useful when dealing with binary
* `grizzled.collection`: Helpers for Scala collections.
* `grizzed.config`: An enhanced INI-style configuration parser, with
  support for include files and variable substitution.
* `grizzled.file`: File-related utility functions.
* `grizzled.io`: Some enhanced I/O functions and classes.
* `grizzled.math`: Some simple math functions, including some common statistics
  functions.
* `grizzled.net`: Network-related stuff, mostly Scala wrappers to simplify or
  extend the Java `java.net` classes.
* `grizzled.parsing`: Classes that aid in certain kinds of parsing.
* `grizzled.reflect`: Some utility functions to help with Scala reflection.
* `grizzled.security`: Some utility front-ends to `java.security`.
* `grizzled.string`: Useful string- and text-related functions.
* `grizzled.string.template`: For substituting variable references within a
  string. Supports both ${var} (Unix-like) and %var% (Windows-like) syntaxes.
* `grizzled.sys`: System-related utilities, akin to Python's `sys` module.
* `grizzled.util`: Miscellaneous utility functions and methods not otherwise
  categorized.
* `grizzled.zip`: Miscellaneous utility functions for writing zip and jar files.

For a more detailed description of what's available, see the
[API documentation][].

## Installation

Grizzled Scala is published to the 
[Bintray Maven repository](https://bintray.com/bmc/maven), which is
automatically linked to Bintray's [JCenter](https://bintray.com/bintray/jcenter)
repository. (From JCenter, it's eventually pushed to the
[Maven Central Repository](http://search.maven.org/).)

**NOTE**: This library uses [semantic versioning](http://semver.org).

- Versions 4.3.0, 4.2.0, 4.1.0, 4.0.0 and 3.1.0 support Scala 2.10, 2.11 and 2.12
- Version 3.0.0 supports Scala 2.10, 2.11 and 2.12.0-RC1
- Versions 2.5.0 and 2.6.0 support Scala 2.10, 2.11 and 2.12.0-M5
- Versions 2.4.x, 2.3.x, 2.2.x, 2.1.0 and 2.0.0 support Scala 2.10, 2.11 and 2.12-M4.
- Version 1.6.1 supports Scala 2.10, 2.11 and 2.12-M4.
- Version 1.6.0 supports Scala 2.10, 2.11 and 2.12-M1.
- Versions 1.5.1, 1.5.0, 1.4.0, 1.3, 1.2 and 1.1.6 support Scala 2.10 and 2.11.
- Versions 1.1.3, 1.1.4 and 1.1.5 support Scala 2.10.
- Version 1.0.14 supports Scala 2.8.0, 2.8.1, 2.8.2, 2.9.0, 2.9.0-1, 2.9.1,
  2.9.1-1 and 2.9.2.

### Installing for Maven

If you're using [Maven][], just specify the artifact, and Maven will do the
rest for you:

* Group ID: `org.clapper`
* Artifact ID: `grizzled-scala_`*scala-version*
* Version: The usual
* Type: `jar`

For example:

    <dependency>
      <groupId>org.clapper</groupId>
      <artifactId>grizzled-scala_2.11</artifactId>
      <version>4.3.0</version>
    </dependency>

If you cannot resolve the artifact, then add the JCenter repository:

    <repositories>
      <repository>
        <snapshots>
          <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>bintray</name>
        <url>http://jcenter.bintray.com</url>
      </repository>
      ...
    </repositories>

For more information on using Maven and Scala, see Josh Suereth's
[Scala Maven Guide][].

### Using with SBT

Add the following to your SBT build:

    libraryDependencies += "org.clapper" %% "grizzled-scala" % "4.3.0"

## Source Code Repository

The source code for the Grizzled Scala Library is maintained on [GitHub][].
To clone the repository, run this command:

    git clone git://github.com/bmc/grizzled-scala.git

## Building from Source

Building the library requires [SBT][] 0.13.x, but you don't have to
install it (unless you're building on Windows). Instead, just use the
`./activator` script at the top level of the repository. The script,
part of [Lightbend Activator](https://www.lightbend.com/activator/download),
automatically downloads the appropriate versions of SBT and Scala for
you. (You _do_ need to have an installed Java JDK. I recommend 1.8.)

You can build with this one simple command:

    bin/activator +compile +test +package

The resulting jar file will be under the top-level `target` directory.

## API Documentation

The Scaladoc-generated the [API documentation][] is available locally.
In addition, you can generate your own version with:

    bin/activator doc

## Change log

The change log for all releases is [here][changelog].

## Author

Brian M. Clapper, [bmc@clapper.org][]

## Copyright and License

The Grizzled Scala Library is copyright &copy; 2009-2017 Brian M. Clapper
and is released under a [BSD License][].

## Patches

I gladly accept patches from their original authors. Feel free to email
patches to me or to fork the [GitHub repository][] and send me a pull
request. Along with any patch you send:

* Please state that the patch is your original work.
* Please indicate that you license the work to the Grizzled-Scala project
  under a [BSD License][].

[BSD License]: license.html
[Scala]: http://www.scala-lang.org/
[API Documentation]: api/
[GitHub repository]: http://github.com/bmc/grizzled-scala
[GitHub]: http://github.com/bmc/
[downloads area]: http://github.com/bmc/grizzled-scala/downloads
[Maven central repository]: http://search.maven.org/
[Scala Maven Guide]: http://www.scala-lang.org/node/345
[Maven]: http://maven.apache.org/
[SBT]: http://code.google.com/p/simple-build-tool
[bmc@clapper.org]: mailto:bmc@clapper.org
[changelog]: https://github.com/bmc/grizzled-scala/blob/master/CHANGELOG.md
[SBT cross-building]: http://code.google.com/p/simple-build-tool/wiki/CrossBuild
[Apache Ivy]: http://ant.apache.org/ivy/
[Library Management Maven/Ivy section]: http://code.google.com/p/simple-build-tool/wiki/LibraryManagement#Maven/Ivy
[SBT Manual]: http://code.google.com/p/simple-build-tool/wiki/DocumentationHome
[SBT-repo-email-thread]: http://groups.google.com/group/simple-build-tool/browse_thread/thread/470bba921252a167
[Grizzled Python]: http://software.clapper.org/grizzled-python/
[Doug Tangren]: http://github.com/softprops/
[ls.implicit.ly]: http://ls.implicit.ly
