---
title: The Grizzled Scala Utility Library
layout: withTOC
---

## Introduction

The Grizzled Scala Library is a general-purpose [Scala][] library with a
variety of different modules and packages. It's roughly organized into
subpackages that group different kinds of utility functions and classes.
Currently, the library is broken into the following modules:

Currently, the library is broken into a number of modules:

* `grizzled.binary`: Some code that's useful when dealing with binary
* `grizzled.cmd`: A framework for building command interpreters, similar (in
  concept) to Python's `cmd` module.
* `grizzled.collection`: Helpers for Scala collections.
* `grizzed.config`: An enhanced INI-style configuration parser, with
  support for include files and variable substitution.
* `grizzled.file`: File-related utility functions.
* `grizzled.io`: Some enhanced I/O functions and classes.
* `grizzled.math`: Some simple math functions.
* `grizzled.net`: Network-related stuff, mostly Scala wrappers to simplify or
  extend the Java `java.net` classes.
* `grizzled.parsing`: Classes that aid in certain kinds of parsing.
* `grizzled.readline`: Front-end API for various readline-like libraries.
* `grizzled.reflect`: Some utility functions to help with Scala reflection.
* `grizzled.string`: Useful string- and text-related functions.
* `grizzled.string.template`: For substituting variable references within a
  string. Supports both ${var} (Unix-like) and %var% (Windows-like) syntaxes.
* `grizzled.sys`: System-related utilities, akin to Python's `sys` module.
* `grizzled.util`: Miscellaneous utility functions and methods not otherwise
  categorized.

For a more detailed description of what's available, see the
[API documentation][].

## Installation

The easiest way to install the Grizzled Scala library is to download a
pre-compiled jar from the [Scala Tools Maven repository][]. However, you
can also get certain build tools to download it for you automatically.

### Installing for Maven

If you're using [Maven][], you can simply tell Maven to get Grizzled Scala
from the [Scala Tools Maven repository][]. The relevant pieces of
information are:

* Group ID: `org.clapper`
* Artifact ID: `grizzled-scala_2.8.0`
* Version: `1.0`
* Type: `jar`
* Repository: `http://www.scala-tools.org/repo-releases/`

For example:

    <repositories>
      <repository>
        <id>scala-tools.org</id>
          <name>Scala-tools Maven2 Repository</name>
          <url>http://scala-tools.org/repo-releases</url>
      </repository>
    </repositories>

    <dependency>
      <groupId>org.clapper</groupId>
      <artifactId>grizzled-scala_2.8.0</artifactId>
      <version>1.0</version>
    </dependency>

For more information on using Maven and Scala, see Josh Suereth's
[Scala Maven Guide][].

### Using with SBT

If you're using [SBT][] (the Simple Build Tool) to compile your code, you
can place the following lines in your project file (i.e., the Scala file in
your `project/build/` directory):

    val grizzled = "org.clapper" %% "grizzled-scala" % "1.0"

**NOTES**

1. The first doubled percent is *not* a typo. It tells SBT to treat
   Grizzled Scala as a cross-built library and automatically inserts the
   Scala version you're using into the artifact ID. It will *only* work if
   you are building with Scala 2.8.0. See the [SBT cross-building][] page
   for details.
2. Prior to Grizzled Scala, version 1.0, you also had to specify the location
   of a custom Maven repository. With version 1.0, however, Grizzled Scala
   is now being published to the [Scala Tools Maven repository][], which
   SBT automatically searches.

## Source Code Repository

The source code for the Grizzled Scala Library is maintained on [GitHub][].
To clone the repository, run this command:

    git clone git://github.com/bmc/grizzled-scala.git

## Building from Source

Building the Grizzled Scala Library requires [SBT][]. Install SBT, as
described at the SBT web site. Then, assuming you have an `sbt` shell script
(or .BAT file, for Windows), run:

    sbt update

That command will pull down the external jars on which the Grizzled Scala
Library depends. After that step, build the library with:

    sbt compile test package

The resulting jar file will be in the top-level `target` directory.

## API Documentation

The Scaladoc-generated the [API documentation][] is available locally.
In addition, you can generate your own version with:

    sbt doc

## Change log

The change log for all releases is [here][changelog].

## Author

Brian M. Clapper, [bmc@clapper.org][]

## Copyright and License

The Grizzled Scala Library is copyright &copy; 2009-2010 Brian M. Clapper
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
[Scala Tools Maven repository]: http://www.scala-tools.org/repo-releases/
[Scala Maven Guide]: http://www.scala-lang.org/node/345
[Maven]: http://maven.apache.org/
[SBT]: http://code.google.com/p/simple-build-tool
[bmc@clapper.org]: mailto:bmc@clapper.org
[changelog]: CHANGELOG.html
[SBT cross-building]: http://code.google.com/p/simple-build-tool/wiki/CrossBuild
[Apache Ivy]: http://ant.apache.org/ivy/
[Library Management Maven/Ivy section]: http://code.google.com/p/simple-build-tool/wiki/LibraryManagement#Maven/Ivy
[SBT Manual]: http://code.google.com/p/simple-build-tool/wiki/DocumentationHome
[SBT-repo-email-thread]: http://groups.google.com/group/simple-build-tool/browse_thread/thread/470bba921252a167
