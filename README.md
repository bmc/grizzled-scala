Grizzled: A general-purpose library of Scala classes and objects
================================================================

This is the Grizzled Scala API, which is similar to the clapper.org
Grizzled Python API, only for Scala. (Duh.) It contains a variety of
miscellaneous utility classes and objects. Basically, whenever I find
myself writing something that's general-purpose, I put it in here, so
I can easily use it in multiple projects.

Currently, the API is broken into a number of modules:

- `grizzled.net`: Network-related stuff, mostly Scala wrappers to simplify or
  extend the Java `java.net` classes.
- `grizzled.string`: Various useful string- and text-related functions.
- `grizzled.file`: File system-related utility functions.
- `grizzled.sys`: System-related utilities, akin to Python's `sys` module.


