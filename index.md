---
title: The Grizzled Scala Utility Library
layout: withTOC
---

[![Build Status](https://travis-ci.org/bmc/grizzled-scala.svg?branch=master)](https://travis-ci.org/bmc/grizzled-scala)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.clapper/grizzled-scala_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.clapper/grizzled-scala_2.11)

## Introduction

The Grizzled Scala Library is a general-purpose [Scala][] library with a
variety of different modules and packages. It's roughly organized
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

- Version 4.7.0 and on support Scala 2.11, Scala 2.12 and Scala 2.13.
- Version 4.6.0 (and any subsequent patch releases) support Scala 2.10 **only**.
- All releases prior to 4.6.0 support Scala 2.10, 2.11 and 2.12.

tl;dr: If you don't need Scala 2.10 support, use 4.7.0 or better. 

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
      <artifactId>grizzled-scala_2.12</artifactId>
      <version>4.9.0</version>
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

    libraryDependencies += "org.clapper" %% "grizzled-scala" % "4.9.0"

## Source Code Repository

The source code for the Grizzled Scala Library is maintained on [GitHub][].
To clone the repository, run this command:

    git clone git://github.com/bmc/grizzled-scala.git

## Building from Source

Building the library requires [SBT][] 1.x and a 1.8 version of the Java JDK.

You can build with this one simple command:

    sbt +compile +test +package

The resulting jar file will be under the top-level `target` directory.

## API Documentation

The Scaladoc-generated the [API documentation][] is available locally.
In addition, you can generate your own version with:

    sbt doc

## Change log

The change log for all releases is [here][changelog].

## Author

Brian M. Clapper, [bmc@clapper.org][]

## Copyright and License

The Grizzled Scala Library is copyright &copy; 2009-2018 Brian M. Clapper
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
