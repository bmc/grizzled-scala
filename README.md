Grizzled: A general-purpose library of Scala classes and objects
================================================================

Introduction
------------

This is the Grizzled Scala Library, which is similar to the clapper.org
Grizzled Python Library, only for Scala. (Duh.) It contains a variety of
miscellaneous utility classes and objects. Basically, whenever I find
myself writing something that's general-purpose, I put it in here, so I can
easily use it in multiple projects.

Currently, the library is broken into a number of modules:

- `grizzled.file`: File system-related utility functions.
- `grizzled.net`: Network-related stuff, mostly Scala wrappers to simplify or
  extend the Java `java.net` classes.
- `grizzled.readline`: Front-end API for various readline-like libraries.
- `grizzled.string`: Various useful string- and text-related functions.
- `grizzled.sys`: System-related utilities, akin to Python's `sys` module.

Building
--------

Building the Grizzled Scala Library requires [SBT] [sbt] (the Simple Build
Tool). Install SBT, as described at the SBT web site. Then, build the
Grizzled Scala Library with:

    sbt compile test package

The resulting jar file will be in the top-level `target` directory.

  [sbt]: http://code.google.com/p/simple-build-tool





