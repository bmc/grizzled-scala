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
<http://bmc.github.com/grizzled-scala/>. Please see that page for
complete details, including installation instructions.
---
Copyright &copy; 2009-2010 Brian M. Clapper, <i>bmc@clapper.org</i>
