# Grizzled Scala Change Log

Version 4.3.0:

* Added `replaceFirstChar()` to `GrizzledString`.
* Added `grizzled.file.util.longestCommonPathPrefix()`
* Updated version of Lightbend Activator.
* Updated version of Scalatest.
* Updated version of SBT.
* Removed copyrights from some more source files, as they're hard to
  maintain.

Version 4.2.0:

* Added `RandomUtil.randomLongBetween()`.

Version 4.1.0:

* Added `grizzled.random.RandomUtil`, containing some random number helper
  functions.
* Changed `grizzled.file.Includer` to use a `List`, internally, rather than
  a `Stack`.
* Added `grizzled.datetime.DateTimeUtil`, with a couple useful datetime-related
  functions.
* Added `grizzled.datetime.Implicits` to provide enrichment classes for
  `java.util.Date`, `java.util.Calendar` and `java.sql.Timestamp`.
* Removed custom matchers trait and `scala.util.Try` custom matcher in testers,
  in favor of ScalaTest's `shouldBe 'success` and `shouldBe 'failure`.

Version 4.0.0:

_Contains breaking API changes._

* Moved implicit evidence objects for `CanReleaseResource` into a sub-object
  (`Implicits`) within the `CanReleaseResource` companion, to alleviate
  confusion in Scala 2.12.0. They should be imported explicitly, not with a
  wildcard import, e.g.,
  `import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable`.
* Added a `CanReleaseResource` evidence parameter for `java.lang.AutoCloseable`.

Version 3.1.0:

* Built for Scala 2.12.0.
* Fixed `grizzled.net.URLUtil` so that downloads don't bail with a
  `java.nio.file.FileSystemNotFoundException` for non-file URLs.

Version 3.0.0:

_Contains breaking API changes._

* Bumped cross-compiled build from Scala 2.12.0-M5 to 2.12.0-RC1.
* Removed `grizzled.either.Implicits.RichEither`. The methods it contains
  are part of `Either` in Scala 2.12 (though are implemented somewhat
  differently).
* The `sub()` method in `grizzled.string.template.StringTemplate` now
  returns a `Try`, instead of an `Either`. 
* `grizzled.parsing.Pushback.pushback(List[T])` is now called 
  `pushbackMany()`.
* Removed the following classes, which have been deprecated for some time:
    - `grizzled.io.RichInputStream`
    - `grizzled.io.RichReader`
    - `grizzled.parsing.IteratorStream`
    - `grizzled.collection.MultiIterator`
* Removed the following methods/functions, which have deprecated for some time:
    - `grizzled.parsing.Pushback.totalRead`
    - `grizzled.parsing.SafeIterator.totalRead`.
    - `grizzled.file.Implicits.GrizzledFile.walk` 
    - `grizzled.sys.systemProperties`

Version 2.8.0:

* Added `grizzled.file.util.joinAndNormalizePath`, which combines the
  functionality of `joinPath` with that of `normalizePath`.
* Fixed `grizzed.file.util.deleteTree()` to return successfully if the
  directory doesn't exist, instead of failing. 

Version 2.7.0:

* Added `tryWithResource`, a version of `grizzled.util.withResource` that
  returns a `Try`.
* Upgraded to ScalaTest 3.0.0, release version.

Version 2.6.0:

* Added an implicit `mapWhile()` function for use with collection types.
* Un-deprecated `grizzled.config.Configuration`. It's still used by tools
  like [AVSL](http://software.clapper.org/avsl)
* Corrected a bug in `grizzled.file.Includer` that prevented the final read
  from completing in certain circumstances.
* 
 
Version 2.5.0:

* Added `grizzled.string.util.longestCommonPrefix()`.
* Bumped cross-compiled build from Scala 2.12.0-M4 to 2.12.0-M5.
* Upgraded [ScalaTest][] to 3.0.0-RC4.

Version 2.4.2:

* Fixed `grizzled.zip.Zipper` so that flattened zips don't end up with a
  spurious "." directory.

Version 2.4.1:

* Deprecated `grizzled.config.Configuration`. If you need a good configuration
  file format, use YAML or [HOCON](https://github.com/typesafehub/config).
* Various fixes for Windows, including:
    - Changed various classes to use use the `line.separator`
      property, rather than a hard-coded "\n", when adding line terminators.
    - Changed how `grizzled.net.URLUtil` resolves parent directories.
* Removed `BrainDeadHTTPServer` (used during testing) as it overly complicates
  testing. `file:` URLs are now used instead.

Version 2.4.0:

* Removed use of `asInstanceOf` in `grizzled.io.Implicits.RichReader` and
  `grizzled.io.Implicits.RichInputStream`.
* Deprecated `grizzled.collection.MultiIterator`. It's not necessary: Use
  the Scala `Iterator` class's `++` operator to combine multiple iterators
  lazily.
* Added tester for `grizzled.io.MultiSource`.
* Reimplemented `grizzled.math.max()` and `grizzled.math.min()` to work
  with any type for which there is an `Ordering`. Added a unit tester for
  them.
* Fleshed out `grizzled.io.SourceReader` implementation and added unit test.
* Converted `grizzled.string.WordWrapper` to be more functional (i.e., got
  rid of all `var` usages).
* Renamed `IteratorStream` (in `grizzled.parsing`) to `SafeIterator`, to avoid
  confusion with Scala streams. `IteratorStream` is now a deprecated alias.
* Deprecated `IteratorStream.count()`, as its functionality is better handled
  by callers that care.
* Converted uses of `var` in `grizzled.string.template` classes to `val`.
* Added some unit tests for the classes in `grizzled.parsing`.
* DOCS: Added package description for `grizzled.zip`.

Version 2.3.1:

* Fixed bug in `Zipper (incorrect comparison between `String` and `Char`).
* Cleaned up various unused imports.

Version 2.3.0:

* Added `grizzled.zip.Zipper` class, which makes creating zip files and jar
  files easier than using the JDK classes or the
  [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/zip.html)
  classes. In particular, `Zipper` simplifies the handling and creation of
  the intermediate directories that contain zip entries. See the docs for
  details.
* `grizzled.io.RichReader` and `grizzled.io.RichInputStream` have `copyTo()`
  methods that previously returned Unit and threw exceptions on error. They
  now return `Try` objects.
* `grizzled.file.GrizzledFile` (an enrichment of `java.io.File`) now supports a
  `pathExists` method that returns a `Try`.

Version 2.2.2:

* Miscellaneous cleanup of unit tests.

Version 2.2.1:

* Rearranged some code to fix a compilation bug (JDK 1.7, Scala 2.11, on Ubuntu)
  that appears to be related to SI-2905.
* Fixed `GrizzledString.translateMetachars` so that it compiles in the Travis CI
  environment. The pattern match was too complicated for that environment, which means it
  might have failed elsewhere, too.

Version 2.2.0:

* Implemented a simpler, more elegant version of
  `GrizzledString.translateMetachars()`.
* `grizzled.config.Configuration` changes:
  - Restored ability of `grizzled.config.Configuration` to expand metacharacters
    in non-raw values.
  - Deprecated the `grizzled.config.Configuration.apply()` functions, in favor
    of `read()` functions that return `Try` (instead of `Either`).
  - Changed `grizzled.config.ValueConverter` to require overriding a `Try`-based
    function, rather than an `Either`-based function.
  - Added implicit value converters for `Double` and `Float`.
  - Restored (broken) ability to specify a "not found" function.

Version 2.1.0:

* Removed unused `lib` directory and its contained `jar` files.
* `grizzled.string.WordWrapper` is now a case class.
* Fixed wrapping in `grizzled.string.WordWrapper` to handle specified "ignore"
  characters in the prefix, not just in the wrapped text.
* Deprecated the implicit conversions in `grizzled.io.RichReader` and
  `grizzled.io.RichInputStream`. Import `grizzled.io.Implicits._` to get the
  same conversions.
* Refactored some of the test classes.
* Added a `tryGet()` method to `grizzled.config.Configuration`, as a move
  toward getting rid of `Either`.

Version 2.0.1:

* Fixed `GrizzledString.translateMetachars`. It was not properly parsing
  Unicode escapes that are embedded in other strings. It would fail on
  "Here\u0160123", for instance.
* Added a `CanReleaseResource` object for `scala.io.Source`, which doesn't
  extend `java.io.Closeable`. This allows `withResource()` to work with
  `scala.io.Source` objects, without the user having to write a custom
  `CanReleaseResource[Source]` object. (Pull request from
  [Adam Lesperance](https://github.com/lespea).)

Version 2.0.0 (Incompatible API changes):

* Added `grizzled.net.URI` and `grizzled.net.URL` front-end case class
  wrappers for the Java equivalents.
* Renamed `grizzled.net.url` to `grizzled.net.URLUtil`. Modified the
  various `download()` methods to return `Future`, and added versions that
  take `grizzled.net.URL`.
* Modified the synchronous `grizzled.net.URLUtil.withDownloadedFile()` to take
  a `timeout` parameter. Added a version that takes `grizzled.net.URL`.
* Added unit tests for `grizzled.net.URLUtil` download functions.
* Added `grizzled.util.Implicits.RichTry` enrichment class.
* Fixes to `grizzled.file.Includer`:
    * Fixed some problems with URI parsing.
    * Fixed uses of `scala.io.Source` and `java.net.URI`, to correct problems
      with detecting the parent location for a relative include.
    * Added comments indicating that doing an `include` from a
      `scala.io.Source` can make it difficult to infer the location of a
      relative include, depending on the underlying resource the `Source`
      is reading.
    * Changed `Includer` companion object constructors to return
      `scala.util.Try`, instead of throwing an exception.
    * Added constructors that take `java.io.File`.
    * Made class constructor private. Use the `Includer.apply()` functions.
    * Added unit tester.
* Removed deprecated `grizzled.readline` package. Use [JLine 2][], instead.
* Removed `grizzled.cmd`. It was a nice idea (based on the concepts in the
  Python [cmd](https://docs.python.org/2/library/cmd.html) module), but
  it's not being used, really.
* Deprecated `grizzled.sys.systemProperties`. Use [scala.sys.SystemProperties][]
  or [scala.util.Properties][], instead.
* Removed deprecated versions of `withCloseable`. Use
  `grizzled.util.withResource`, instead.
* Remove the following deprecated objects. The corresponding implicit classes
  remain, but they're enabled by importing from a corresponding `Implicits`
  object (for consistency).
    * `grizzled.collection.GrizzledIterable`
    * `grizzled.collection.GrizzledLinearSeq`
    * `grizzled.collection.CollectionIterator`
    * `grizzled.file.GrizzledFile`
    * `grizzled.io.GrizzledSource`
    * `grizzled.string.GrizzledChar`
    * `grizzled.string.GrizzledString`
* Fixed implementation of `grizzled.math.stats.mode` to be purely immutable
  (i.e., not to use a mutable `Map` under the covers) and to use the
  `Collection` API's `max` method, instead of rolling its own.

[scala.sys.SystemProperties]: http://www.scala-lang.org/api/current/#scala.sys.SystemProperties
[scala.util.Properties]: http://www.scala-lang.org/api/current/#scala.util.Properties$

Version 1.6.1

* Added an explicit conversion from `grizzled.net.IPAddress` to
  `java.net.InetAddress`. An explicit conversion already exists for the
  other direction, and implicit conversions already exist in both directions.
* Built against Scala 2.12-M4, instead of -M1.

Version 1.6.0:

* Now compiles against Scala 2.12, as well as 2.11 and 2.10.
* Added `activator`, for a self-bootstrapping build.
* Removed unused dependency on `scala-async` and `scala-actors`.
* Added a better message when a `CanReleaseResource` implicit evidence
  parameter cannot be found for `grizzled.util.withCloseable()`.
* Added `GrizzledString.escapeNonPrintables` and `GrizzledChar.isPrintable`
  methods.
* Added `toTry` method to the rich `Either` class, to convert an
  `Either` to a `Try`.

Version 1.5.1:

* Cleaned up some warnings identified by IntelliJ.
* Deprecated `grizzled.util.withCloseable()` and
  `grizzled.io.util.withCloseable()` in favor of
  `grizzled.util.withResource()`.
* Added some unit tests for the `grizzled.file.util.eglob()` and
  `grizzled.file.util.glob()` functions.
* Converted remaining unit tests from ScalaTest's `FunSuite` to `FlatSpec`.
* Deprecated enrichment classes that aren't inside objects called
  `Implicits`, and added `Implicits` objects where appropriate, for
  consistency.

Version 1.5.0:

* Modified `grizzled.string.WordWrapper` to allow specification of
  characters that should be ignored when calculating wrapping.
* Removed some deprecated methods and classes that have been around for awhile.
* Added `+`, `++`, `-` and `--` methods to `grizzled.config.Configuration`, to
  allow addition to and removal from a `Configuration` in an immutable way.
* Updated ScalaTest to 2.2.6 and scala-async to 0.9.5.

Version 1.4.0:

* Added `grizzled.net.IPAddress` functions and methods to handle converting IP
  addresses to and from numeric values.
* Added a version of `IPAddress.apply()` that takes a `java.net.InetAddress`.
* Added `grizzled.net.IPAddress.parseAddress()`, to parse a non-hostname IP
  address without incurring a DNS lookup penalty.
* Cleaned up code in `grizzled.net.IPAddress`.
* Converted various functions to return `Try` instead of `Either`.
* Removed use of deprecated Scala `IterableProxy` trait.
* Deprecated the `grizzled.readline` package. Use
  [JLine2](https://github.com/jline/jline2), instead.
* Build cleanups: Removed SBT "ls" plugin and removed stray repos.
* Cleaned up Scaladoc warnings.

Version 1.3:

* `grizzled.config.Configuration` wasn't properly handling custom regular
  expressions for parsing section names and comments. Fixed by
  [Stefan Schlott (@Skyr)](https://github.com/Skyr).

Version 1.2:

* `grizzled.config.Configuration` is now completely immutable. Specifically,
  the following changes have been made:
  - Explicit conversion methods (e.g., `getInt()`, `getBoolean()`) have
    been removed, in favor of new `asOpt[T]()` and `asEither[T]()` methods.
    These new methods take an implicit `ValueConverter` object, allowing
    callers to specify their own type conversions. Some predefined converters
    are available in the `grizzled.config.Configuration.Implicits` package.
  - It is now possible to specify a "not found" handler, when constructing
    a `Configuration` object, to handle cases where an option was not found.
  - The `Configuration` class is now final, because the constructor is now
    private.
  - All `Configuration` objects must be instantiated through the companion
    `Configuration` object.
  - All methods throwing exceptions have either been deprecated or removed.
    Implication: Non-safe configuration objects (i.e., those that throw
    exceptions for variables that cannot be substituted) are not supported.
    However, `asEither()` will properly handle that situation.
  - Most exceptions have been removed, except those still thrown by deprecated
    methods.
* `grizzled.string.util.stringToBoolean()` is deprecated, as it throws an
  exception. Use the new `str2Boolean()` instead; it returns an `Either`.
* Removed exceptions from `grizzled.string.StringTemplate`. The `substitute()`
  method is now deprecated (because it throws exceptions) in favor of the
  `sub()` method (which returns an `Either`).
* `grizzled.url.download()` now returns an `Either`, instead of throwing an
  exception on error.
* The functions in `grizzled.file.util` and the methods in
  `grizzled.file.GrizzledFile` now return `Either`, instead of throwing
  exceptions.
* Implicits in the `grizzled.net.inet` class are now in a special `Implicits`
  object.

Version 1.1.6:

* Removed the `grizzled.generator` package, as it relies on the unsupported
  and unmaintained Scala continuations plugin.
* Changed `grizzled.file.util.listRecursively()` to use Scala `Stream` objects,
  which permit lazy evaluation. It's a better, and simpler, solution than
  continuation passing.
* Now cross-compiled for Scala 2.11.
* Published to Bintray.
* Updated to SBT 0.13.2

Version 1.1.5:

* Added `grizzled.either`, which contains enrichments for the `Either`
  class, including `map()` and `flatMap()` methods that map when the
  value is `Right` (and permits easier use of `Either` objects in
  `for` comprehensions).
* Increased file copying speed in `grizzled.io.RichInputStream` by
  adding buffering. (Fix supplied by [Jim Fulton](https://github.com/jimfulton)
* `grizzled.math.stats.range()` of a single value now returns 0, as it should.
  Addresses [Issue #4](https://github.com/bmc/grizzled-scala/issues/4)
* Upgraded to latest version of ScalaTest.

Version 1.1.4:

* Added `grizzled.string.util.bytesToHexString`.
* Added `grizzled.security.MessageDigest`, a simplified interface to the
  Java `MessageDigest` capability.

Version 1.1.3:

* API documentation changes.
* Built for Scala 2.10 release.
* Removed a bunch of deprecated methods.
* Updated ScalaTest version.
* Addressed [Issue #4](https://github.com/bmc/grizzled-scala/issues/4):
  `stats.range()` broken when passed a single value.

Version 1.1.2:

* Cross-compiled and published for Scala 2.10.0-RC1.
* Converted to use ScalaTest 2.0, which changes `expect` to `expectResult`.

Version 1.1.1:

* Re-integrated Doug Tangren's (outstanding) [ls](http://ls.implicit.ly/) SBT plugin.

Version 1.1.0:

* Built for the Scala 2.10.0 series _only_ (2.10.0-M7, initially). **This
  version, and later versions, are 2.10-only. 2.9.x and earlier will be
  supported via the 1.0.x release branch.** This is due to changes in the
  Scala library between 2.9 and 2.10.
* Converted code to use 2.10 reflection API.
* Added `-feature` to `scalac` options, and removed all feature warnings.
  In many cases, this simply necessitating importing various `scala.language`
  packages, such as `scala.language.reflectiveCalls` and
  `scala.language.implicitConversions`.
* Removed use of `val` in `for` comprehensions, as it's now deprecated.
* Upgraded build to SBT 0.12.
* Moved `GrizzledFile.listRecursively()` functionality from `GrizzledFile`
  (which is intended to enhance `java.io.File` implicitly) to
  `grizzled.file.util` package, where it is more easily invoked directly.
  Replaced `GrizzledFile.listRecursively()` with a simple wrapper that
  invokes `grizzled.file.util.listRecursively()`.
* Converted use of `scala.collection.JavaConversions.IterableWrapper` (which
  is deprecated in 2.10) to `scala.collection.convert.JIterableWrapper`.

Version 1.0.14:

* Addressed [Issue #4](https://github.com/bmc/grizzled-scala/issues/4):
  `stats.range()` broken when passed a single value.

Version 1.0.13:

* Cross-compiled for Scala 2.9.2.

Version 1.0.12:

* Readline implementation now uses [Jline 2][].
* Cross-compiled for Scala 2.9.1-1.

Version 1.0.11.1:

* Cross-compiled for Scala 2.8.2.

Version 1.0.11:

* Fixed cross-compilation issues. Grizzled-Scala is, once again, cross-
  compiled and cross-published for 2.8.0, 2.8.1, 2.9.0, 2.9.0-1 and 2.9.1.

Version 1.0.10:

* Fixed `grizzled.sys.makeNativePath` and related functions to treat the
  Mac platform the same as Posix, instead of throwing an exception.
* Updated to use SBT 0.11.2.
* Now publishes artifacts to `oss.sonatype.org`. Artifacts are signed with
  GPG key, as a result.

Version 1.0.9:

* Fixed `grizzled.readline` so that a newline in the prompt doesn't return
  `None` (for EOF).
* Based on [pull request #3][], by [Dan Sully][], added the following
  features to `grizzled.config`:
  - Section name regular expression can now be specified to `Configuration`
    objects, thus allowing alternate section name forms.
  - Comment regular expression can now be specified to `Configuration`,
    allowing alternate comment syntaxes.
  - `Configuration` now supports at `getAsList()` method, which returns
    a value split into a string list. Delimiters may be specified. The
    value returned is of type `Option[List[String]]`.

[pull request #3]: https://github.com/bmc/grizzled-scala/pull/3
[Dan Sully]: https://github.com/dsully

Version 1.0.8:

* Fixed an off-by-one error in `grizzled.collection.ListIterator`
* Cleaned up Scaladocs considerably.
* Converted code to confirm with standard Scala coding style.
* Now builds for [Scala][] 2.9.1, as well as 2.9.0-1, 2.9.0, 2.8.1 and 2.8.0.

Version 1.0.7:

* Now builds against Scala 2.9.0.1, as well as Scala 2.9.0, 2.8.1 and 2.8.0.
* Converted to build with [SBT][] 0.10.1

Version 1.0.6:

* Now builds against Scala 2.9.0, as well as Scala 2.8.0 and 2.8.1.
* Updated to version 1.4.1 of [ScalaTest][] for Scala 2.9.0. (Still uses
  ScalaTest 1.3, for Scala 2.8).
* Updated to use [SBT][] 0.7.7.
* Removed various deprecated methods.
* Corrected implementation of `grizzled.reflect.isOfType` for non-primitives.

Version 1.0.5:

* Miscellaneous internal cleanup of `Configuration` and `grizzled.readline`
  code.
* Updated to version 1.3 of [ScalaTest][].

[ScalaTest]: http://www.scalatest.org/


Version 1.0.4:

* Fixed some error messages in the `Configuration` class, per an email from
  *brian.ewins /at/ gmail.com*.

Version 1.0.3:

* Now builds against [Scala][] 2.8.1 and 2.8.0.
* Added `range()` function to the `grizzled.math.stats` module.
* Enhanced `grizzled.readline` module to permit the caller-supplied
  `Completer` object to specify the delimiters to use when tokenizing an
  input line for tab completion.
* Enhanced `grizzled.cmd` module to allow the caller to instantiate the
  `CommandInterpreter` class with the set of delimiters to use when tokenizing
  an input line for tab completion.

[Scala]: http://www.scala-lang.org/

Version 1.0.2:

* Added the `grizzled.math.stats` module, which contains some common
  statistics functions.
* Scaladoc documentation is now generated with a proper title.
* Fixed problem where `grizzled.cmd` failed to find a Readline library,
  because of an inadvertent change of a constant from a `def` to a `val`.
* Now compiles against [Scala][] 2.8.1 RC2, as well as 2.8.0.

[http://www.nmr.mgh.harvard.edu/Neural_Systems_Group/gary/python.html]: http://www.nmr.mgh.harvard.edu/Neural_Systems_Group/gary/python.html

Version 1.0.1:

* Now compiles against [Scala][] 2.8.1 RC1, as well as 2.8.0

Version 1.0:

* Now published to the [Scala Tools Nexus][] repository, so it's no
  longer necessary to specify a custom repository to find this artifact.

Version 0.7.4:

* Added `grizzled.reflect` module and `grizzled.reflect.isOfType()` method,
  which uses `scala.reflect.Manifest` to simplify erasure-proof type tests.
  e.g.:

        def test(v: Any) =
        {
            import grizzled.reflect._
            if (isOfType[List[Int]](v))
                ...
            else if (isOfType[List[Char]](v))
                ...
            ...
        }

* Moved `grizzled.parsing.markup` to the new, separate [MarkWrap][]
  library. Among other things, this move keeps the Grizzled Scala library
  more focused and reduces transitive dependencies.
* Removed most explicit matches against `Some` and `None`, making better
  use of the Scala API.
* Updated to released 1.2 version of [ScalaTest][].
* Changed dependency on [ScalaTest][] to be a test-only dependency.

Version 0.7.3:

* Updated to build with Scala 2.8.0.final *only*.

[Knockoff]: http://tristanhunt.com/projects/knockoff/

Version 0.7.2:

* Updated to [Knockoff][] version 0.7.2-13, which corrects some Markdown
  translation bugs.
* Updated to Scala 2.8.0.RC5. Now builds against RC3 and RC5 only.

Version 0.7.1:

* Bumped to [SBT][] version 0.7.4.
* Added `relativePath` method to `GrizzledFile`.
* Added ability to "parse" (i.e., emit) plain text and HTML/XHTML to the
  `grizzled.parsing.markup` package.
* Updated to [Knockoff][] version 0.7.1-12, which corrects some Markdown
  translation bugs.
* Fixed `grizzled-scala` artifact publishing bug ([issue #1][]).
* Removed support for Scala 2.8.0.RC2.
* Changed SBT publishing to use an SSH key file, to eliminate the Ivy
  Swing prompt.

[issue #1]: http://github.com/bmc/grizzled-scala/issues/issue/1

Version 0.7:

* Added `grizzled.io.GrizzledSource`, which extends `scala.io.Source` with
  mixin methods.
* Deprecated `grizzled.string.implicits` and `grizzled.file.implicits`
  modules, in favor of more granular imports. See the
  `grizzled.file.GrizzledFile`, `grizzled.string.GrizzledString` and
  `grizzled.string.GrizzledChar` companion objects for details.
* Deprecated the string-to-boolean implicit function, in favor of the
  more explicit `grizzled.string.util.stringToBoolean()` method.
* Changed `GrizzledFile.listRecursively` to take an optional
  `topdown` flag, indicating whether directory traversal should be top-down
  or bottom-up.
* Deprecated `grizzled.parsing.Markdown` in favor of new
  `grizzled.parsing.markup` module.
* Add [Textile][] support to `grizzled.parsing.markup`, via the Eclipse
  [WikiText][] library.
* Changed `grizzled.parsing.markup` to use Tristan Juricek's [Knockoff][]
  library for [Markdown][], rather than invoking the [Showdown][]
  JavaScript parser via [Rhino][].
* Now compiles under Scala 2.8.0.RC3 and RC2. Dropped support for RC1.

Version 0.6:

* Added `findReadline()` convenience method to `grizzled.readline.Readline`.
  This method attempts to find and load a suitable Readline library.
* Cleaned up `grizzled.file.util.deleteTree` method.
* Added versions of `grizzled.file.util.copyFile`,
  `grizzled.file.util.copyTree`, and `grizzled.file.util.deleteTree` that
  take `java.io.File` objects.
* Replaced `grizzled.io.useThenClose` with the more flexible
  `grizzled.io.withCloseable`. (`useThenClose` is still present, but
  it's deprecated.)
* Added `copyTo()` method to `grizzled.file.GrizzledFile`, which can be
  implicitly mixed into `java.io.File`.
* Ensured that various supposedly tail-recursive methods are marked with
  `@tailrec`, to be sure.
* Maven artifact now includes Scala version (e.g., `grizzled-scala_2.8.0.RC2`,
  instead of `grizzled-scala`).
* Updated to build against Scala 2.8.0.RC2, as well as Scala 2.8.0.RC1.

Version 0.5.1:

* Updated to `posterous-sbt` plugin version 0.1.5.
* Removed CHANGELOG, because it can now be generated by `posterous-sbt`.
* Added `grizzled.generator`, which can be used to create Python-style
  generators. (It borrows shamelessly from
  [Rich Dougherty's Stack Overflow post][].)
* Added `listRecursively()` generator function to `grizzled.file.GrizzledFile`.
  Via implicits, `GrizzledFile` can be used to extend `java.io.File`.
* The `grizzled.readline.Readline` trait now contains a `cleanup` method,
  and the `grizzled.cmd.CommandInterpreter` class now calls it on exit.
  This change ensures that the terminal isn't left in a weird state.

[Rich Dougherty's Stack Overflow post]: http://stackoverflow.com/questions/2201882/implementing-yield-yield-return-using-scala-continuations/2215182#2215182

Version 0.5:

* Updated to Scala 2.8.0.RC1.
* Replaced uses of now-deprecated `Math` functions with corresponding functions
  from `scala.math`.
* Enhanced `grizzled.config.Configuration`:
  - A `forMatchingSections()` method allows simple looping over sections that
    match a regular expression.
  - A `matchingSections()` methods returns a sequence of sections that match
    a regular expression.
  - The `options()` method now returns an empty map if the section doesn't
    exist, instead of throwing an exception.
  - A new `setOption()` method overwrites an option value, if it exists already,
    instead of throwing a `DuplicateOptionException`, as `addOption()` does.
  - `ConfigurationReader` is deprecated, and the logic to read a configuration
    file has been moved into the `Configuration` class, to permit easier
    subclassing.
  - Now supports a "safe load" mode, where exceptions aren't thrown.
  - Added unit tests for the `Configuration` class.
  - Added ability to retrieve converted integer and boolean option values.
  - The `option()` methods are now deprecated, in favor of new `get()` and
    `getOrElse()` methods that are modeled after their `Map` counterparts.
  - Added a new `getSection()` method that returns an `Option[Section]`.

Version 0.4.2:

* Updated to [SBT][] version 0.7.3.
* Added `withDownloadedFile()` to `grizzled.net.url`, to execute a block on
  a downloaded URL.
* The `grizzled.io.implicits` module has been replaced by individual
  modules, for more granular scope control (e.g.,
  `grizzled.io.RichInputStream`, `grizzled.io.RichReader`)
* The `grizzled.io` package has been split into individual source files.
* Added new `grizzled.io.SourceReader` class that wraps a `scala.io.Source`
  inside a `java.io.Reader`.

Version 0.4.1:

* Fixed inadvertent bug in `grizzled.cmd` command handling, when commands
  span multiple lines.

Version 0.4:

* Added `grizzled.collection.GrizzledLinearSeq` and related implicits, as a
  place to put additional methods for sequences.
* Added `grizzled.collection.GrizzledLinearSeq.columnarize()`, which takes a
  sequence (e.g., a list), converts its contents to strings, and returns a
  single string with the sequence's contents arranged in columns.
* Rearranged the locations of various implicit functions, so callers can
  have finer-grained control over which ones are in scope.
* `grizzled.editline.EditLine` now shows its completions in columnar format.
* Added `BlockCommandHandler` to `grizzled`.cmd, to handle commands consisting
  of blocks of lines between a start and end line.
* Added `HiddenCommandHandler` to `grizzled.cmd`, allowing special commands
  that are not displayed in the help.
* Changed EOF handling in `grizzled.cmd` slightly.
* Added `createTemporaryDirectory()` and `withTemporaryDirectory()` to
  `grizzled.file.util` module.
* Added `isEmpty` to `grizzled.file.GrizzledFile` (which can be implicitly
  converted to and from `java.io.File`).
* Fixed problem with prefix handling in `grizzled.string.WordWrapper`.
* Now uses [SBT][] 0.7.2 to build from source.

[sbt]: http://code.google.com/p/simple-build-tool

Version 0.3.1:

`grizzled.cmd` changes:

* "history" command now uses the syntax `history [-n] [regex]`
   where *n* is the maximum number of entries to show, and *regex* is a
   regular expression to filter history entries.

* Some commands starting with "." were being incorrectly limited to a
  single character (e.g., ".r", but not ".read").

* General clean-up and bug fixing in the "redo command" handler.

* `CommandHandler` classes can now exempt themselves from the history.

* The `RedoCommandHandler` (which handles the "r" and "!" commands) now
  exempts itself from the history.

Version 0.3:

* Converted to Scala 2.8.0
* Now must be compiled with [SBT][sbt] version 0.7.0 or better.
* Fixed tail call optimization problem in `grizzled.io.RichInputStream`.
  Thanks to Claudio Bley (*cbley /at/ av-test.de*)
* Added grizzled.parsing.MarkdownParser, for parsing Markdown documents.
  (Currently uses the [Showdown][Showdown] Javascript library, via
  [Mozilla Rhino][Rhino].)
* `grizzled.cmd.HelpHandler` now supports a ".help" alias.
* Added `grizzled.util.withCloseable` control structure.
* The grizzled.readline API now uses the [Java EditLine][javaeditline]
  wrapper for the Unix EditLine library, instead of the one in
  Java-Readline. implementation, instead of the one in Java-Readline.
  Completion handling is more reliable with the Java Editline
  implementation.
* grizzled.cmd now tries to load EditLine first.


Version 0.2:

* In `grizzled.cmd`, the default handler for the "help" command now does
  tab completion on the existing commands.
* Changed the way `grizzled.readline` exposes completion context. Instead
  of exposing a cursor, it exposes a tokenized list, with a special
  `Cursor` token. This approach fits better with Scala's pattern matching.
* `grizzled.collection.MultiIterator` is now covariant, not invariant.
* Miscellaneous internal changes where `yield` is used.
* Changed license to New BSD License.

Version 0.1:

* Initial release.
* Fixed an off-by-one error in `grizzled.collection.ListIterator`
* Cleaned up Scaladocs considerably.
* Converted code to confirm with standard Scala coding style.
* Now builds for [Scala][] 2.9.1, as well as 2.9.0-1, 2.9.0, 2.8.1 and 2.8.0.


[javaeditline]: http://www.clapper.org/software/java/javaeditline/
[Jline 2]: https://github.com/huynhjl/jline2
[Knockoff]: http://tristanhunt.com/projects/knockoff/
[Showdown]: http://attacklab.net/showdown/
[Markdown]: http://daringfireball.net/projects/markdown/
[MarkWrap]: http://bmc.github.com/markwrap/
[Rhino]: http://www.mozilla.org/rhino/
[SBT]: http://www.scala-sbt.org/
[Scala Tools Nexus]: http://nexus.scala-tools.org/content/repositories/releases
[Scala]: http://www.scala-lang.org/
[ScalaTest]: http://scalatest.org/
[Textile]: http://textile.thresholdstate.com/
[WikiText]: http://help.eclipse.org/ganymede/index.jsp?topic=/org.eclipse.mylyn.wikitext.help.ui/help/devguide/WikiText%20Developer%20Guide.html
