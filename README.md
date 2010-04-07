Grizzled: A general-purpose library of Scala classes and objects
================================================================

## Introduction

This is the Grizzled Scala Library, which is similar to the clapper.org
Grizzled Python Library, only for Scala. (Duh.) It contains a variety of
miscellaneous utility classes and objects. Basically, whenever I find
myself writing something that's general-purpose, I put it in here, so I can
easily use it in multiple projects.

Currently, the library is broken into a number of modules:

- `grizzled.binary`: Some code that's useful when dealing with binary
- `grizzled.cmd`: A framework for building command interpreters, similar (in
  concept) to Python's `cmd` module.
- `grizzled.collection`: Helpers for Scala collections
- `grizzed.config`: An enhanced INI-style configuration parser
- `grizzled.file`: File system-related utility functions.
- `grizzled.io`: Some enhanced I/O functions and classes
- `grizzled.math`: Some simple enhanced math functions
- `grizzled.net`: Network-related stuff, mostly Scala wrappers to simplify or
  extend the Java `java.net` classes.
- `grizzled.parsing`: Classes that aid in certain parsing logic
- `grizzled.readline`: Front-end API for various readline-like libraries.
- `grizzled.string`: Various useful string- and text-related functions.
- `grizzled.sys`: System-related utilities, akin to Python's `sys` module.

The home page for the Grizzled Scala Library is
<http://bmc.github.com/grizzled-scala/>

## Getting this Plugin

### The Released Version

In your own project, create a project file in `project/build/` file, if you
haven't already. (See the [SBT Wiki][sbt-wiki] for details on how to configure
your project with a custom build script.)

[SBT Wiki]: http://code.google.com/p/simple-build-tool/wiki/BuildConfiguration

Add the following lines, to make your project depend on Grizzled:

    val orgClapperMavenRepo = "clapper.org Maven Repo" at "http://maven.clapper.org/"

    val grizzled = "org.clapper" % "grizzled-scala" % "0.4.2"

### The Development Version

You can also use the development version of this plugin (that is, the
version checked into the [GitHub repository][github-repo]), by building it
locally.

First, download the plugin's source code by cloning this repository.

    git clone http://github.com/bmc/grizzled-scala.git

Then, within the `markdown` project directory, publish it locally:

    sbt update publish-local

[github-repo]: http://github.com/bmc/grizzled-scala

## Building

Building the Grizzled Scala Library requires [SBT] [sbt] (the Simple Build
Tool), version 0.7.0 or better. Install SBT 0.7.0, as described in
the [SBT wiki] [sbt-setup]. Then, run

    sbt update

to pull down the external dependencies. After that step, build the Grizzled
Scala Library with:

    sbt compile test package

The resulting jar file will be in the top-level `target` directory.

  [sbt]: http://code.google.com/p/simple-build-tool
  [sbt-setup]: http://code.google.com/p/simple-build-tool/wiki/Setup
---
Copyright &copy; 2009-2010 Brian M. Clapper, <i>bmc@clapper.org</i>
