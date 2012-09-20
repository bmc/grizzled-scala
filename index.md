---
title: The Grizzled Scala Utility Library
layout: withTOC
---

## Introduction

The Grizzled Scala Library is a general-purpose [Scala][] library with a
variety of different modules and packages. It's similar to the clapper.org
[Grizzled Python][] Library, only for Scala. (Duh.) It's roughly organized
into subpackages that group different kinds of utility functions and
classes. Currently, the library is broken into the following modules:

Currently, the library is broken into a number of modules:

* `grizzled.binary`: Some code that's useful when dealing with binary
* `grizzled.cmd`: A framework for building command interpreters, similar (in
  concept) to Python's `cmd` module.
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

Grizzled Scala is published to the `oss.sonatype.org` repository;  Sonatype
automatically pushes the artifacts to the  [Maven central repository][]. You
can get download the Grizzled Scala library jar directly from the Maven
repository. However, if you're using [Maven][] or [SBT][], you can just have
those tools do the job for you.

- Version 1.1.0 supports Scala 2.10.0-M7
- Version 1.0.13 supports Scala 2.8.0, 2.8.1, 2.8.2, 2.9.0, 2.9.0-1, 2.9.1, 
  2.9.1-1 and 2.9.2.

### Installing for Maven

If you're using [Maven][], just specify the artifact, and Maven will do the
rest for you:

* Group ID: `org.clapper`
* Artifact ID: `grizzled-scala_2.9.2` or `grizzled-scala_2.10`
* Version: `1.0.13` or `1.1.0`
* Type: `jar`

For example, to get the 2.10.0-M7 build:

    <dependency>
      <groupId>org.clapper</groupId>
      <artifactId>grizzled-scala_2.10</artifactId>
      <version>1.1.0</version>
    </dependency>

For more information on using Maven and Scala, see Josh Suereth's
[Scala Maven Guide][].

### Using with SBT

#### 0.7.x

If you're using [SBT][] 0.7.x to compile your code, you can place the
following line in your project file (i.e., the Scala file in your
`project/build/` directory):

    val grizzled = "org.clapper" %% "grizzled-scala" % "1.0.13"

#### 0.11.x and 0.12.x

If you're using [SBT][] 0.11.x to compile your code, you can use the
following line in your `build.sbt` file (for Quick Configuration). If
you're using an SBT 0.11.x Full Configuration, you're obviously smart
enough to figure out what to do, on your own.

If you're using Scala 2.9.2 or earlier:

    libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.0.13"

If you're using Scala 2.10.0-M7:

    libraryDependencies += "org.clapper" % "grizzled-scala_2.10" % "1.1.0"

Grizzled Scala is also registered with [Doug Tangren][]'s excellent
[ls.implicit.ly][] catalog. If you use the `ls` SBT plugin, you can install
Grizzled Scala with

    sbt> ls-install grizzled-scala

## Source Code Repository

The source code for the Grizzled Scala Library is maintained on [GitHub][].
To clone the repository, run this command:

    git clone git://github.com/bmc/grizzled-scala.git

## Building from Source

Building the Grizzled Scala Library requires [SBT][] 0.11.1 or better.
Install SBT, as described at the SBT web site. Then, assuming you have an
`sbt` shell script (or .BAT file, for Windows), run:

    sbt compile test package

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

The Grizzled Scala Library is copyright &copy; 2009-2012 Brian M. Clapper
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
