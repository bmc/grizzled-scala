---
title: The Grizzled Scala Utility Library
layout: withTOC
---

## Introduction

The Grizzled Scala Library is a general-purpose [Scala][] library with a
variety of different modules and packages. It's roughly organized into
subpackages that group different kinds of utility functions and classes.
For a sampling of what's available, see the [API documentation][].

## Installation

The easiest way to install the Grizzled Scala library is to download a
pre-compiled jar from the [*clapper.org* Maven repository][]. However, you
can also get certain build tools to download it for you.

### Installing for Maven

If you're using [Maven][], you can get Grizzled Scala from the
[*clapper.org* Maven Repository][]. The relevant pieces of information are:

* Group ID: `clapper.org`
* Artifact ID: `grizzled-scala`
* Version: `0.5`
* Type: `jar`
* Repository: `http://maven.clapper.org/`

Creating the appropriate Maven configuration items is left as an exercise
for the reader. (One of the things I like about using [SBT][] is that I
never have to look at Maven XML.)

### Using with SBT

If you're using [SBT][] (the Simple Build Tool) to compile your code, you
can place the following lines in your project file (i.e., the Scala file in
your `project/build/` directory):

    val orgClapperRepo = "clapper.org Maven Repository" at
        "http://maven.clapper.org"
    val grizzled = "org.clapper" % "grizzled-scala" % "0.4.2"

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
[*clapper.org* Maven repository]: http://maven.clapper.org/org/clapper/
[Maven]: http://maven.apache.org/
[SBT]: http://code.google.com/p/simple-build-tool
[bmc@clapper.org]: mailto:bmc@clapper.org
