package grizzled.file

import scala.annotation.tailrec
import scala.language.higherKinds
import grizzled.io.Implicits.RichInputStream
import grizzled.sys.os
import grizzled.sys.OperatingSystem._
import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}
import java.security.{SecureRandom => Random}

import scala.util.{Failure, Success, Try}

class FileDoesNotExistException(message: String) extends Exception

/** Useful file-related utility functions.
  */
object util {
  val fileSeparator: String = File.separator
  val fileSeparatorChar: Char = fileSeparator(0)

  private lazy val random = new Random()

  import grizzled.ScalaCompat._

  // -------------------------------------------------------------------------
  // Public Methods
  // -------------------------------------------------------------------------

  /** Get the directory name of a pathname.
    *
    * @param path     path (absolute or relative)
    * @param fileSep  the file separator to use. Defaults to the value of
    *                 the "file.separator" property.
    *
    * @return the directory portion
    */
  def dirname(path: String, fileSep: String = fileSeparator): String = {
    val components = splitPath(path, fileSep)
    components match {
      case Nil =>
        ""

      case List("") =>
        ""

      case simple :: Nil if ! (simple startsWith fileSep) =>
        "."

      case _ =>
        val len = components.length
        val result = components.take(len - 1) mkString fileSep
        if (result.length == 0)
          fileSep
        else
          result
    }
  }

  /** Get the basename (file name only) part of a path.
    *
    * @param path     the path (absolute or relative)
    * @param fileSep  the file separator to use. Defaults to the value of
    *                 the "file.separator" property.
    *
    * @return the file name portion
    */
  def basename(path: String, fileSep: String = fileSeparator): String = {
    val components = splitPath(path, fileSep)
    components match {
      case Nil =>
        ""
      case List("") =>
        ""
      case simple :: Nil if ! (simple startsWith fileSep) =>
        path
      case _ =>
        components.drop(components.length - 1) mkString fileSep
    }
  }

  /** Split a path into directory (dirname) and file (basename) components.
    * Analogous to Python's `os.path.pathsplit()` function.
    *
    * @param path     the path to split
    * @param fileSep  the file separator to use. Defaults to the value of
    *                 the "file.separator" property.
    *
    * @return a (dirname, basename) tuple of strings
    */
  def dirnameBasename(path:    String,
                      fileSep: String = fileSeparator): (String, String) = {
    if (Option(path).isEmpty || path.isEmpty)
      ("", "")
    else if ((path == ".") || (path == ".."))
      (path, "")
    else if (! (path contains fileSep))
      (".", path)
    else if (path == fileSep)
      (fileSep, "")
    else  {
      // Use a character to split, so it's not interpreted as a regular
      // expression (which causes problems with a Windows-style "\".
      // NOTE: We deliberately don't use splitPath() here.
      val components = (path split fileSep(0)).toList
      components match {
        case Nil =>
          ("", "")
        case head :: Nil =>
          (head, "")
        case head :: tail =>
          val listTuple = components splitAt (components.length - 1)
          val s: String = listTuple._1 mkString fileSep
          val prefix =
            if ((s.length == 0) && (path startsWith fileSep))
              fileSep
            else
              s
          (prefix, listTuple._2 mkString fileSep)
      }
    }
  }

  private lazy val ExtRegexp = """^(.*)(\.[^.]+)$""".r

  /** Split a pathname into the directory name, basename, and extension
    * pieces.
    *
    * @param pathname the pathname
    * @param fileSep  the file separator to use. Defaults to the value of
    *                 the "file.separator" property.
    *
    * @return a 3-tuple of (dirname, basename, extension)
    */
  def dirnameBasenameExtension(pathname: String,
                               fileSep: String = fileSeparator):
    (String, String, String) = {

    val (path1, extension) = pathname match {
      case ExtRegexp(pathNoExt, ext) => (pathNoExt, ext)
      case _                         => (pathname, "")
    }

    val (dirname, basename) = dirnameBasename(path1, fileSep)
    (dirname, basename, extension)
  }

  /** Return the current working directory, as an absolute path.
    *
    * @return the current working directory
    */
  def pwd: String = new File(".").getCanonicalPath

  /**
   * Calculate the relative path between two files.
   *
   * @param from  the starting file
   * @param to    the file to be converted to a relative path
   *
   * @return the (String) relative path
   */
  def relativePath(from: File, to: File): String = {
    if (from.getAbsolutePath == to.getAbsolutePath)
      basename(from.getPath)

    else {
      val fromPath = toPathArray(from)
      val toPath = toPathArray(to)
      val commonLength = commonPrefix(fromPath, toPath)
      val relativeTo = toPath.drop(commonLength)
      if (fromPath.length == commonLength)
        // It's right under the from path.
        relativeTo mkString fileSeparator
      else {
        val commonParentsTotal = fromPath.length - commonLength - 1
          require(commonParentsTotal >= 0)
        val up = (".." + fileSeparator) * commonParentsTotal
        relativeTo.mkString(up, fileSeparator, "")
      }
    }
  }

  /** Return a list of paths matching a pathname pattern. The pattern may
    * contain simple shell-style wildcards. See `fnmatch()`. This function
    * is essentially a direct port of the Python `glob.glob()` function.
    *
    * Restrictions:
    *
    * - There's currently no way to escape a wildcard character. That is,
    *   if you need to match a '*' character or a '?' character exactly,
    *   you can't do that with this library. (You can't do it with the
    *   `glob` library in Python 2 or Python 3, either.)
    *
    * @param path  The path to expand.
    *
    * @return a list of possibly expanded file names
    */
  def glob(path: String): List[String] = {

    def glob1(dirname: String, pattern: String): List[String] = {
      val dir = if (dirname.length == 0) pwd else dirname
      val names = Option(new File(dir).list).map(_.toList)
                                            .getOrElse(List.empty[String])

      if (names.isEmpty)
        Nil

      else {
        val names2 =
          if (path(0) != '.')
            names.filter(_(0) != '.')
          else
            names

        names.filter(n => fnmatch(n, pattern))
      }
    }

    def glob0(dirname: String, basename: String): List[String] = {
      if (basename.length == 0) {
        if (new File(dirname).isDirectory)
          List(basename)
        else
          Nil
      }
        else {
          val path = dirname + fileSeparator + basename
          if (new File(path).exists())
            List(basename)
          else
            Nil
        }
    }

    val wildcards = """[\*\?\[]""".r
    if ((wildcards findFirstIn path).isEmpty) {
      if (new File(path).exists)
        List(path)
      else
        List.empty[String]
    }
    else {
      val (dirname, basename) = dirnameBasename(path)
      if (dirname.length == 0)
        for (name <- glob1(pwd, basename)) yield name

      else {
        val dirs = if ((wildcards findFirstIn dirname).nonEmpty)
          glob(dirname)
        else
          List(dirname)
        val globber = if ((wildcards findFirstIn basename).nonEmpty)
          glob1 _
        else
          glob0 _

        for (d <- dirs; name <- globber(d, basename))
          yield d + fileSeparator + name
      }
    }
  }

  /** An extended ''glob'' function that supports all the wildcards of
    * the `glob()` function, in addition to:

    *  - a leading `~`, signifying the user's home directory
    *  - a special `**` wildcard that recursively matches any directory.
    *    (Think "ant".)
    *
    * ''~user'' is not supported, however.
    *
    * NOTE: Any non-Windows operating system is treated as Posix.
    *
    * @param pattern   the wildcard pattern
    *
    * @return list of matches, or an empty list for none
    */
  def eglob(pattern: String): List[String] = {
    def doGlob(pieces: List[String], directory: String): List[String] = {
      import scala.collection.mutable.ArrayBuffer

      val result = new ArrayBuffer[String]()

      pieces match {
        case Nil => pieces
        case head :: tail =>
          val last = tail.isEmpty

          if (head == "**") {
            val remainingPieces = if (last) Nil else pieces.drop(1)

            for ((root, dirs, files) <- walk(directory, topdown = true)) {
              if (last) {
                // At the end of a pattern, "**" just recursively
                // matches directories.
                result += root
              }

              else {
                // Recurse downward, trying to match the rest of
                // the pattern.
                result ++= doGlob(remainingPieces, root)
              }
            }
          }

          else {
            // Regular glob pattern.

            val path = directory + fileSeparator + head
            val matches = glob(path)
            if (matches.nonEmpty) {
              if (last)
              // Save the matches, and stop.
                result ++= matches

              else {
                // Must continue recursing.
                val remainingPieces = pieces.drop(1)
                for (m <- matches if new File(m).isDirectory) {
                  val subResult = doGlob(remainingPieces, m)
                  for (partialPath <- subResult)
                    result += partialPath
                }
              }
            }
          }
      }

      result.toList
    }

    // Main eglob() logic

    // Account for leading "~"
    val adjustedPattern =
      if (pattern.length == 0)
        "."
      else if (pattern.startsWith("~"))
        normalizePath(joinPath(System.getProperty("user.home"), pattern drop 1))
      else
        pattern

    // Determine leading directory, which is different per OS (because
    // of Windows' stupid drive letters).
    val (relativePattern, directory) = eglobPatternSplitter(adjustedPattern)

    // Do the actual globbing.
    val pieces = splitPath(relativePattern)
    val matches = doGlob(pieces, directory)

    matches map normalizePath
  }


  /** Similar to Python's `fnmatch()` function, this function determines
    * whether a string matches a wildcard pattern. Patterns are Unix-style
    * shell-style wildcards:
    *
    *  - `*` matches everything
    *  - `?` matches any single character
    *  - `[set]` matches any character in ''set''
    *  - `[!set]` matches any character not in ''set''
    *
    * An initial period in `filename` is not special. Matches are
    * case-sensitive on Posix operating systems, case-insensitive elsewhere.
    *
    * @param name    the name to match
    * @param pattern the wildcard pattern
    */
  def fnmatch(name: String, pattern: String): Boolean = {
    // Convert to regular expression pattern.

    val caseConv: String => String =
      if (os == Posix)
        {s => s}
      else
        {s => s.toLowerCase}

    val regex = caseConv("^" + pattern.replace("\\", "\\\\")
                         .replace(".", "\\.")
                         .replace("*", ".*")
                         .replace("[!", "[^")
                         .replace("?", ".") + "$").r
    regex.findFirstIn(caseConv(name)).nonEmpty
  }

  /** Directory tree generator, adapted from Python's `os.walk()`
    * function.
    *
    * Note that `java.nio.Files.walk()` and `java.nio.Files.walkTree()`
    * provide similar functionality, though with a different interface.
    *
    * For each directory in the directory tree rooted at top (including top
    * itself, but excluding '.' and '..'), yields a 3-tuple
    *
    * {{{
    * (dirpath, dirnames, filenames)
    * }}}
    *
    * ''dirpath'' is a string, the path to the directory. ''dirnames'' is a
    * list of the names of the subdirectories in ''dirpath'' (excluding '.'
    * and '..'). ''filenames'' is a list of the names of the non-directory
    * files in ''dirpath''. Note that the names in the lists are just names,
    * with no path components. To get a full path (which begins with top) to a
    * file or directory in ''dirpath'', use `dirpath + java.io.fileSeparator +
    * name`, or use `joinPath()`.
    *
    * If ''topdown'' is `true`, the triple for a directory is generated before
    * the triples for any of its subdirectories (directories are generated top
    * down). If `topdown` is `false`, the triple for a directory is generated
    * after the triples for all of its subdirectories (directories are generated
    * bottom up).
    *
    * '''WARNING!''' This method does ''not'' grok symbolic links!
    *
    * The JDK's [[https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#walk-java.nio.file.Path-int-java.nio.file.FileVisitOption...- java.nio.file.Files.walk()]]
    * function provides a similar capability in JDK 8. Prior to JDK 8, you can
    * also use  [[https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#walkFileTree(java.nio.file.Path,%20java.util.Set,%20int,%20java.nio.file.FileVisitor) java.nio.file.Files.walkFileTree()]]
    *
    * @param top     name of starting directory
    * @param topdown `true` to do a top-down traversal, `false`
    *                otherwise.
    *
    * @return List of triplets, as described above.
    */
  def walk(top: String, topdown: Boolean = true):
    List[(String, List[String], List[String])] = {
    // This needs to be made more efficient, with some kind of generator.
    import scala.collection.mutable.ArrayBuffer

    val dirs = new ArrayBuffer[String]()
    val nondirs = new ArrayBuffer[String]()
    val result = new ArrayBuffer[(String, List[String], List[String])]()
    val fTop = new File(top)
    val names = Option(fTop.list).getOrElse(Array.empty[String])

    if (names.nonEmpty) {
      for (name <- names) {
        val f = new File(top + fileSeparator + name)
        if (f.isDirectory)
          dirs += name
        else
          nondirs += name
      }

      if (topdown)
        result += ((top, dirs.toList, nondirs.toList))

      for (name <- dirs)
        result ++= walk(top + fileSeparator + name, topdown)

      if (! topdown)
        result += ((top, dirs.toList, nondirs.toList))
    }

    result.toList
  }

  /** List a directory recursively, returning `File` objects for each file
    * (and subdirectory) found. This method does lazy evaluation, instead
    * of calculating everything up-front, as `walk()` does.
    *
    * The JDK's `java.io.Files.walk()` and `java.io.Files.walkTree()`
    * functions provide a similar capability, starting in JDK 8.
    *
    * @param file    The `File` object, presumed to represent a directory.
    * @param topdown If `true` (the default), the stream will be generated
    *                top down. If `false`, it'll be generated bottom-up.
    *
    * @return a stream of `File` objects.
    */
  def listRecursively(file: File, topdown: Boolean = true): LazyList[File] = {

    def go(list: List[File]): LazyList[File] = {
      // See http://www.nurkiewicz.com/2013/05/lazy-sequences-in-scala-and-clojure.html
      list match {
        case Nil => LazyList.empty[File]

        case f :: tail =>
          val list = if (f.isDirectory) f.listFiles.toList else Nil
          if (topdown)
            f #:: go(list ++ tail)
          else
            go(list ++ tail) :+ f
      }
    }

    if (file.isDirectory)
      go(file.listFiles.toList)
    else
      LazyList.empty[File]
  }
  /** Split a path into its constituent components. If the path is
    * absolute, the first piece will have a file separator in the
    * beginning. Examples:
    *
    * <table border="1">
    *   <tr>
    *     <th>Input</th>
    *     <th>Output</th>
    *   </tr>
    *   <tr>
    *     <td class="code">""</td>
    *      <td class="code">List("")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"/"</td>
    *     <td class="code">List("/")
    *   </tr>
    *   <tr>
    *     <td class="code">"foo"</td>
    *     <td class="code">List("foo")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"foo/bar"</td>
    *     <td class="code">List("foo", "bar")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"."</td>
    *     <td class="code">List(".")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"../foo"</td>
    *     <td class="code">List("..", "foo")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"./foo"</td>
    *     <td class="code">List(".", "foo")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"/foo/bar/baz"</td>
    *     <td class="code">List("/foo", "bar", "baz")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"foo/bar/baz"</td>
    *     <td class="code">List("foo", "bar", "baz")</td>
    *   </tr>
    *   <tr>
    *     <td class="code">"/foo"</td>
    *     <td class="code">List("/foo")</td>
    *   </tr>
    * </table>
    *
    * @param path    the path
    * @param fileSep the file separator to use. Defaults to the value of
    *                the "file.separator" property.
    *
    * @return the component pieces.
    */
  def splitPath(path: String, fileSep: String = fileSeparator): List[String] = {
    // Split with the path separator character, rather than the path
    // separator string. Using the string causes Scala to interpret it
    // as a regular expression, which causes problems when the separator
    // is a backslash (as on Windows). We could escape the backslash,
    // but it's just as easy to split on the character, not the string,

    // Null guard.

    val nonNullPath = Option(path).getOrElse("")

    // Special case for Windows. (Stupid drive letters.)

    val (prefix, usePath) =
      if (fileSep == "\\")
        splitDrivePath(nonNullPath)
      else
        ("", nonNullPath)

        // If there are leading file separator characters, split() will
        // produce extra empty array elements. Prevent that.

        val subpath = usePath.foldLeft("")  {
          (c1, c2) => // Note: c1 and c2 are strings, not characters

            if (c1 == fileSep)
              c2.toString
            else
              s"$c1${c2.toString}"
        }

    val absolute = path.startsWith(fileSep) || (prefix != "")

    // Split on the character, not the full string. Splitting on a string
    // uses regular expression semantics, which will fail if this is
    // Windows (and the file separator is "\"). Windows is a pain in the
    // ass.
    val pieces = (subpath split fileSep(0)).toList
    if (absolute) {
      pieces match {
        case Nil          => List[String](prefix + fileSep)
        case head :: tail => (prefix + fileSep + head) :: tail
      }
    }
    else
      pieces
  }

  /** Join components of a path together, using the specified file separator.
    * Note that `java.nio.file.Paths.get()` can be used to join paths, as
    * well.
    *
    * @param fileSep the file separator to use
    * @param pieces  path pieces
    *
    * @return a composite path
    */
  def joinPath(fileSep: String, pieces: Seq[String]): String =
    pieces mkString fileSep

  /** Join components of a path together, using the current file separator.
    * Note that `java.nio.file.Paths.get()` can be used to join paths, as
    * well.
    *
    * @param pieces  path pieces
    *
    * @return a composite path
    */
  def joinPath(pieces: String*): String =
    joinPath(fileSeparator, pieces.toList)

  /** Join components of a path together, using the current file separator.
    * Note that `java.nio.file.Paths.get()` can be used to join paths, as
    * well.
    *
    * @param pieces  path pieces
    *
    * @return a composite path
    */
  def joinPath(pieces: File*): File =
    new File(joinPath(fileSeparator, pieces.toList.map(_.getName)))

  /** Join components of a path together, using the current file separator;
    * then, normalize the result.
    *
    * @param pieces  path pieces
    *
    * @return a composite, normalized path
    *
    * @see [[joinPath(pieces:String*):String*]]
    * @see [[normalizePath]]
    */
  def joinAndNormalizePath(pieces: String*): String = {
    normalizePath(joinPath(pieces: _*))
  }

  /** Join components of a path together, using the current file separator;
    * then, normalize the result.
    *
    * @param pieces  path pieces
    *
    * @return a composite, normalized path
    *
    * @see [[joinPath(pieces:String*):String*]]
    * @see [[normalizePath]]
    */
  def joinAndNormalizePath(pieces: File*): File = {
    new File(normalizePath(joinPath(pieces: _*).getPath))
  }

  /** Determine the temporary directory to use.
    *
    * @return the temporary directory
    */
  def temporaryDirectory: File = {
    import grizzled.sys.OperatingSystem._

    def guess: String = {
      grizzled.sys.os match {
        case Posix => "/tmp"
        case Mac   => "/tmp"
        case (Windows | WindowsCE | OS2 | NetWare) => """C:\TEMP"""
        case _  => "/tmp"
      }
    }

    val sysProp = System getProperty "java.io.tmpdir"
    val tempDirName = Option(sysProp).getOrElse(guess)
    new File(tempDirName)
  }

  /** Create a temporary directory. This is now just a simple front-end
    * to `java.nio.file.Files.createTempDirectory()`, and it's deprecated.
    * Use `java.nio.file.Files.createTempDirectory()` directly.
    *
    * @param prefix    Prefix for directory name
    * @param maxTries  Maximum number of times to try creating the
    *                  directory before giving up. '''Ignored now.'''
    *
    * @return A `Success` of the directory, or a `Failure` with any error
    *         that occurs.
    */
  @deprecated("Use java.nio.file.Files.createTempDirectory()", "4.10.0")
  def makeTemporaryDirectory(prefix: String, maxTries: Int = 3): Try[File] = {
    Try {
      Files.createTempDirectory(prefix).toFile
    }
  }

  /** Allow execution of a block of code within the context of a temporary
    * directory. The temporary directory is cleaned up after the operation
    * completes. This version throws exceptions. Use
    * [[tryWithTemporaryDirectory]] if you'd prefer a `Try`.
    *
    * @param prefix  file name prefix to use
    * @param action  action to perform
    *
    * @return whatever the action returns. Errors are thrown as `IOException`.
    */
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def withTemporaryDirectory[T](prefix: String)(action: File => T): T = {
    import grizzled.file.Implicits._

    Try { Files.createTempDirectory(prefix) } match {
      case Failure(ex) => throw ex
      case Success(dir) =>
        try {
          action(dir.toFile)
        }
        finally {
          dir.toFile.deleteRecursively()
        }
    }
  }

  /** Similar to [[withTemporaryDirectory]], this function creates a temporary
    * directory, runs a block of code, and recursively removes the temporary
    * directory when the code block returns. Unlike [[withTemporaryDirectory]],
    * `tryWithTemporaryDirectory`:
    *
    * - returns a `Try`, instead of throwing or propagating exceptions
    * - passes a `java.nio.files.Path` to the code block, instead of a
    *   `java.io.File`
    *
    * @param prefix  the desired prefix to use when generating the directory
    *                name
    * @param action  the code to run
    * @tparam        T the type of the code's return value
    *
    * @return A `Success` containing the result of the code block, or a
    *         `Failure` if an exception is thrown.
    */
  def tryWithTemporaryDirectory[T](prefix: String)(action: Path => T): Try[T] = {
    import grizzled.file.Implicits._

    Try { Files.createTempDirectory(prefix) }.flatMap { dir: Path =>
      val res = Try { action(dir) }
      dir.toFile.deleteRecursively()
      res
    }
  }

  /** Copy multiple files to a target directory. Also see the version of this
    * method that takes only one file.
    *
    * @param files        An `Iterable` of file names to be copied
    * @param targetDir    Path name to target directory
    * @param createTarget `true` to create the target directory,
    *                     `false` to throw an exception if the
    *                     directory doesn't already exist.
    *
    * @return `Success(true)` if the copy worked. `Failure(exception)` on
    *         error.
    */
  @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
  def copy(files: Iterable[String],
           targetDir: String,
           createTarget: Boolean = true): Try[Boolean] = {
    val target = new File(targetDir)

    if ((! target.exists()) && createTarget && (! target.mkdirs())) {
      Failure(new IOException(
        s"""Unable to create target directory "$targetDir"."""
      ))
    }
    else if (target.exists() && (! target.isDirectory)) {
      Failure(new IOException(
        s"""Cannot copy files to non-directory "$targetDir"."""
      ))
    }
    else if (! target.exists()) {
      Failure(new FileDoesNotExistException(
        s"""Target directory "$targetDir" does not exist."""
      ))
    }
    else {
      val results = for (file <- files) yield {
        // The .get() forces a failure if a specific copy fails.
        copyFile(file, targetDir + fileSeparator + basename(file))
      }

      results.toSeq.filter(_.isFailure) match {
        case Seq(head, _*)  => Try { head.get }.map { _ => false }
        case s if s.isEmpty => Success(true)
      }

    }
  }

  /** Copy a file to a directory. The JDK's
    * [[https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#copy(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.CopyOption...) java.nio.file.Files.copy()]]
    * function provides a similar capability.
    *
    * @param file         Path name of the file to copy
    * @param targetDir    Path name to target directory
    * @param createTarget `true` to create the target directory,
    *                     `false` to throw an exception if the
    *                     directory doesn't already exist.
    *
    * @return `Success(true)` if the copy worked. `Failure(exception)` on
    *         error.
    */
  def copy(file:         String,
           targetDir:    String,
           createTarget: Boolean): Try[Boolean] = {
    copy(List[String](file), targetDir, createTarget)
  }

  /** Copy a file to a directory. If the target directory does not exist,
    * it is created. The JDK's
    * [[https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#copy(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.CopyOption...) java.nio.file.Files.copy()]]
    * function provides a similar capability.
    *
    * @param file         Path name of the file to copy
    * @param targetDir    Path name to target directory
    *
    * @return `Success(true)` if the copy worked. `Failure(exception)` on
    *         error.
    */
  def copy(file: String, targetDir: String): Try[Boolean] = {
    copy(file, targetDir, createTarget = true)
  }

  /** Copy a source file to a target file, using binary copying. The source
    * file must be a file. The target path can be a file or a directory; if
    * it is a directory, the target file will have the same base name as
    * as the source file.
    *
    * The JDK's
    * [[https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#copy(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.CopyOption...) java.nio.file.Files.copy()]]
    * function provides a similar capability.
    *
    * @param sourcePath  path to the source file
    * @param targetPath  path to the target file or directory
    *
    * @return A `Success` with the full path of the target file, or
    *         `Failure(exception)`
    */
  def copyFile(sourcePath: String, targetPath: String): Try[String] = {
    copyFile(new File(sourcePath), new File(targetPath)).map {_.getPath}
  }

  /** Copy a source file to a target file, using binary copying. The source
    * file must be a file. The target path can be a file or a directory; if
    * it is a directory, the target file will have the same base name as
    * as the source file.
    *
    * The JDK's `java.nio.file.Files.copy()` function provides a similar
    * capability.
    *
    * @param source  path to the source file
    * @param target  path to the target file or directory
    *
    * @return A `Success` containing the full path of the target file,
    *         or `Failure(exception)`
    */
  def copyFile(source: File, target: File): Try[File] = {
    import java.io.{BufferedInputStream, BufferedOutputStream,
                    FileInputStream, FileOutputStream}
    import grizzled.util.withResource

    val targetFile =
      if (target.isDirectory)
        new File(joinPath(target.getPath, basename(source.getName)))
      else
        target

    Try {
      import grizzled.util.CanReleaseResource.Implicits.CanReleaseCloseable

      withResource(new BufferedInputStream(new FileInputStream(source))) { in =>
        withResource(new BufferedOutputStream(new FileOutputStream(targetFile))) { out =>
          in.copyTo(out)
          targetFile
        }
      }
    }
  }

  /** Recursively copy a source directory and its contents to a target
    * directory. Creates the target directory if it does not exist.
    *
    * @param sourceDir  the source directory
    * @param targetDir  the target directory
    *
    * @return `Success(true)` if the copy worked. `Failure(exception)` on
    *         error.
    */
  def copyTree(sourceDir: String, targetDir: String): Try[Boolean] =
    copyTree(new File(sourceDir), new File(targetDir))

  /** Recursively copy a source directory and its contents to a target
    * directory. Creates the target directory if it does not exist.
    *
    * @param sourceDir  the source directory
    * @param targetDir  the target directory
    *
    * @return `Success(true)` if the copy worked. `Failure(exception)` on
    *         error.
    */
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def copyTree(sourceDir: File, targetDir: File): Try[Boolean] = {
    if (! sourceDir.exists()) {
      Failure(new FileDoesNotExistException(sourceDir.getPath))
    }
    else if (! sourceDir.isDirectory) {
      Failure(new IOException(
        s"""Source directory "${sourceDir.getPath}" is not a directory."""
      ))
    }
    else {
      val files = sourceDir
        .list
        .map { f: String =>
          (new File(sourceDir, f), new File(targetDir, f))
        }

      targetDir.mkdirs

      // Not sure why Wart Remover is complaining about inferred Any here,
      // since everything is explicit.
      files.foreach { case (src: File, target: File) =>
        if (src.isDirectory)
          copyTree(src, target)
        else
          copyFile(src, target)
      }

      Success(true)
    }
  }

  /** Recursively copy a source directory and its contents to a target
    * directory. Creates the target directory if it does not exist.
    *
    * @param sourceDir  the source directory
    * @param targetDir  the target directory
    *
    * @return `Success(true)` if the copy worked. `Failure(exception)` on
    *         error.
    */
  def copyTree(sourceDir: Path, targetDir: Path): Try[Boolean] = {
    if (! Files.exists(sourceDir)) {
      Failure(new FileDoesNotExistException(sourceDir.toAbsolutePath.toString))
    }
    else if (! Files.isDirectory(sourceDir)) {
      Failure(new IOException(
        s"""Source "${sourceDir.toAbsolutePath.toString}" is not a directory."""
      ))
    }
    else {
      import grizzled.ScalaCompat.CollectionConverters._

      Files.createDirectories(targetDir)

      val targetAsString = targetDir.toString

      Files
        .walk(sourceDir)
        .iterator
        .asScala
        .filter { path: Path => ! Files.isDirectory(path) }
        .foreach { path: Path =>
          // These paths contain the parent directory.
          val src = sourceDir.toString
          val p = path.toString

          val minusParent = if (p startsWith src) p.drop(src.length + 1) else p
          val targetFile = Paths.get(targetAsString, minusParent)
          val targetParent = dirname(targetFile.toString)
          Files.createDirectories(Paths.get(targetParent))
          Files.copy(path, targetFile)
        }

      Success(true)
    }
  }

  /** Recursively remove a directory tree. This function is conceptually
    * equivalent to `rm -r` on a Unix system.
    *
    * @param dir  The directory
    */
  def deleteTree(dir: String): Unit = deleteTree(new File(dir))

  /** Recursively remove a directory tree. This function is conceptually
    * equivalent to `rm -r` on a Unix system.
    *
    * @param dir The directory
    *
    * @return `Failure(exception)` on error, `Success(total)` on success. `total`
    *         is the total number of deleted files.
    */
  def deleteTree(dir: File): Try[Int] = {
    def deleteOne(f: File): Try[Int] = {
      if (! f.delete)
        Failure(new IOException(s"Can't delete '${f.toString}'"))
      else
        Success(1)
    }

    if (! dir.exists)
      Success(0)
    else if (! dir.isDirectory)
      Failure(new IOException(s""""${dir.toString}" is not a directory."""))
    else {
      @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
      val treeResults = dir.listFiles.map { f =>

        val t = Try {
          if (f.isDirectory)
            deleteTree(f)
          else
            deleteOne(f)
        }

        // Force an abort on first failure.
        t.get
      }

      treeResults.find { t => t.isFailure }.getOrElse {
        deleteOne(dir)
      }
    }
  }

  /** Recursively remove a directory tree. This function is conceptually
    * equivalent to `rm -r` on a Unix system.
    *
    * @param dir The directory
    *
    * @return `Failure(exception)` on error, `Success(total)` on success. `total`
    *         is the total number of deleted files.
    */
  def deleteTree(dir: Path): Try[Int] = deleteTree(dir.toFile)

  /** Similar to the Unix ''touch'' command, this function:
    *
    *  - updates the access and modification times for any existing files
    *    in a list of files
    *  - creates an nonexistent files in the list
    *
    * If any file in the list is a directory, this method will return an error.
    *
    * @param files  Iterable of files to touch
    * @param time   Set the last-modified time to this time, or to the current
    *               time if this parameter is negative.
    *
    * @return `Failure(exception)` on error. `Success(total)` on success.
    */
  def touchMany(files: Iterable[String], time: Long = -1): Try[Int] = {

    val useTime = if (time < 0) System.currentTimeMillis else time
    Try {
      @SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
      val results = files.map { name =>
        // Force a failure on error.
        touch(name, time).get
        1
      }

      results.sum
    }
  }

  /** Similar to the Unix `touch` command, this function:
    *
    *  - updates the access and modification times for a file
    *  - creates the file if it does not exist
    *
    * If the file is a directory, this method will return an error.
    *
    * @param path  The file to touch
    * @param time  Set the last-modified time to this time, or to the current
    *              time if this parameter is negative.
    *
    * @return `Failure(exception)` on error, `Success(true)` on success
    */
  def touch(path: String, time: Long = -1): Try[Boolean] = {
    val file = new File(path)
    if (file.isDirectory)
      Failure(new Exception(s"""File "$path" is a directory."""))

    else if (! file.exists) {
      Try {
        file.createNewFile()
      }
      .flatMap { succeeded =>
        if (! succeeded)
          Failure(new IOException(s"""Unable to create "$path""""))
        else
          Success(true)
      }
    }

    else {
      val useTime = if (time < 0) System.currentTimeMillis else time
      if (! file.setLastModified(useTime))
        Failure(new IOException(s"""Unable to set time on "$path""""))
      else
        Success(true)
    }
  }

  private lazy val DrivePathPattern = "^([A-Za-z]?:)?(.*)$".r

  /** Split a Windows-style path into drive name and path portions.
    *
    * @param path  the path
    *
    * @return a (drive, path) tuple, either component of which can be
    * *       an empty string
    */
  def splitDrivePath(path: String): (String, String) = {
    path match {
      case DrivePathPattern(driveSpec, subPath) =>
        Option(driveSpec)
          .map {
            case ":" => ("", subPath)
            case _   => (driveSpec, subPath)
          }
          .getOrElse(("", subPath))

      case _ => ("", path)
    }
  }

  /** Converts a path name from its operating system-specific format to a
    * universal path notation. Universal path notation always uses a
    * Unix-style "/" to separate path elements. A universal path can be
    * converted to a native (operating system-specific) path via the
    * `native_path()` function. Note that on POSIX-compliant systems,
    * this function simply returns the `path` parameter unmodified.
    *
    * @param path the path to convert to universal path notation
    *
    * @return the universal path
    */
  def universalPath(path: String): String = makeUniversalPath(path)

  /** Converts a path name from universal path notation to the operating
     * system-specific format. Universal path notation always uses a
     * Unix-style "/" to separate path elements. A native path can be
     * converted to a universal path via the `universal_path()`
     * function. Note that on POSIX-compliant systems, this function simply
     * returns the `path` parameter unmodified.
     *
     * @param path the path to convert from universtal to native path notation
     *
     * @return the native path
     */
  def nativePath(path: String): String = makeNativePath(path)

  /** Normalize a path, eliminating double slashes, resolving embedded
    * ".." strings (e.g., "/foo/../bar" becomes "/bar"), etc. Works for
    * Windows and Posix operating systems.
    *
    * @param path  the path
    *
    * @return the normalized path
    */
  def normalizePath(path: String): String = doPathNormalizing(path)

  /** Normalize a path, eliminating double slashes, resolving embedded
    * ".." strings (e.g., "/foo/../bar" becomes "/bar"), etc. Works for
    * Windows and Posix operating systems.
    *
    * @param path  the path
    *
    * @return the normalized path
    */
  def normalizePath(path: Path): Path = {
    Paths.get(doPathNormalizing(path.toString))
  }

  /** Normalize a Windows path name. Handles UNC paths. Adapted from the
    * Python version of normpath() in Python's `os.ntpath` module.
    *
    * @param path   the path
    *
    * @return the normalized path
    */
  def normalizeWindowsPath(path: String): String = {
    // We need to be careful here. If the prefix is empty, and the path
    // starts with a backslash, it could either be an absolute path on
    // the current drive (\dir1\dir2\file) or a UNC filename
    // (\\server\mount\dir1\file). It is therefore imperative NOT to
    // collapse multiple backslashes blindly in that case. The code
    // below preserves multiple backslashes when there is no drive
    // letter. This means that the invalid filename \\\a\b is preserved
    // unchanged, where a\\\b is normalized to a\b.

    val (prefix, newPath) = splitDrivePath(path) match {
      case ("", subPath) =>
        // No drive letter - preserve initial backslashes

        (subPath takeWhile (_ == '\\') mkString "",
         subPath dropWhile (_ == '\\') mkString "")

      case (pfx, subPath) =>
        // We have a drive letter.

        (pfx + "\\", subPath dropWhile (_ == '\\') mkString "")
    }

    // Normalize the path pieces. Note: normalizePathPieces() doesn't
    // handle leading ".." in an absolute path, such as "\\..\\..". We
    // handle that later.
    val piecesTemp = normalizePathPieces(newPath.split("\\\\").toList)

    // Remove any leading ".." that shouldn't be there.
    val newPieces =
      if (prefix == "\\")
        piecesTemp dropWhile (_ == "..")
      else
        piecesTemp

    // If the path is now empty, substitute ".".
    if ((prefix.length == 0) && newPieces.isEmpty)
      "."
    else
      prefix + (newPieces mkString "\\")
  }

  /** Adapted from the Python version of normpath() in Python's
    *  `os.posixpath` module.
    *
    * @param path   the path
    *
    * @return the normalized path
    */
  def normalizePosixPath(path: String): String = {
    path match {
      case ""  => "."
      case "." => "."
      case _   =>
        // POSIX allows one or two initial slashes, but treats
        // three or more as a single slash. We don't do that here.
        // Two initial slashes is also collapsed into one.

        val initialSlashes =
          if (path.startsWith("/"))
            1
          else
            0

        // Normalize the path pieces. Note: normalizePathPieces()
        // doesn't handle leading ".." in an absolute path, such as
        // "/../..". We handle that later.
        //
        // Note: Must also account for a single leading ".", which
        // must be preserved
        val pieces =
          path.split("/").toList match {
            case Nil              => Nil
            case "." :: remainder => remainder
            case other            => other
          }

        val normalizedPieces1 = normalizePathPieces(pieces)

        // Remove any leading ".." that shouldn't be there.
        val normalizedPieces2 =
          if (path startsWith "/")
            normalizedPieces1 dropWhile (_ == "..")
          else
            normalizedPieces1

        val result =
          ("/" * initialSlashes) + (normalizedPieces2 mkString "/")

        // An empty string is "."
        if (result == "")
          "."
        else
          result
    }
  }

  /** Find the longest common path prefix from a list of paths. Based on
    * [[https://rosettacode.org/wiki/Find_common_directory_path#Advanced]]
    *
    * @param paths the paths
    *
    * @return the longest common path, which might be the empty string
    */
  // reduceLeft() is okay here, since we're checking the size first.
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def longestCommonPathPrefix(paths: List[String]): String = {
    val PathSep = "/"
    val BoundaryRe = s"(?=[$PathSep])(?<=[^$PathSep])|(?=[^$PathSep])(?<=[$PathSep])"

    def common(a: List[String], b: List[String]): List[String] = {
      (a, b) match {
        case (a :: as, b :: bs) if a equals b => a :: common(as, bs)
        case _ => Nil
      }
    }

    if (paths.length < 2) {
      paths.headOption.getOrElse("")
    }
    else {
      val uPaths = paths
      val res = paths
        // Convert all paths to "universal" paths (i.e., with "/" characters,
        // even if we're on Windows). We'll convert back when we're done.
        .map(universalPath)
        // Split on path boundaries
        .map { _.split(BoundaryRe).toList }
        // Find the common prefix
        .reduceLeft(common)
        // Rebuild
        .mkString

      // Convert back to a native path
      nativePath(res)
    }
  }

  // -------------------------------------------------------------------------
  // Private Methods
  // -------------------------------------------------------------------------

  /** Convert a file into a path array. Borrowed from SBT source code.
    */
  private def toPathArray(file: File): Array[String] = {
    @tailrec def toPathList(f: File, current: List[String]): List[String] = {
      // Can't use map, to preserve tail-recursion.
      Option(f) match {
        case Some(f2) => toPathList(f2.getParentFile, f2.getName :: current)
        case None => current
      }
    }

    toPathList(file.getCanonicalFile, Nil).toArray
  }

  /** Get the length of the common prefix between two arrays.
    */
  private def commonPrefix[T](a: Array[T], b: Array[T]): Int = {
    @tailrec def common(count: Int): Int = {
      if ((count >= a.length) || (count >= b.length) || (a(count) != b(count)))
        count
      else
        common(count + 1)
    }

    common(0)
  }

  /** For the eglob algorithm to work, the pattern needs to be split into a
    * (directory, subpattern) pair, where the subpattern is relative. This
    * splitting operating is operating system-dependent, largely because
    * of Windows' stupid drive letters. This variable holds a partially
    * applied function for the splitter, determined the first time it is
    * referenced. That way, eglob() doesn't do this same match on every
    * call.
    */
  private lazy val eglobPatternSplitter = os match {
    case (Mac | Posix) => splitPosixEglobPattern _
    case Windows       => splitWindowsEglobPattern _
    case _             => splitPosixEglobPattern _
  }

  /** Windows pattern splitter for eglob(). See description for the
    * eglobPatternSplitter value, above.
    *
    * @param pattern  the pattern to split
    *
    * @return a (directory, subpattern) tuple
    */
  private def splitWindowsEglobPattern(pattern: String): (String, String) = {
    splitDrivePath(pattern) match {
      case ("", "") =>
        (".", ".")

      case ("", path) =>
        (path, ".")

      case (drive, "") =>
        (".", drive)

      case (drive, path) =>
        // Hack: Can't handle non-absolute paths in a drive.
        // Pretend a drive letter means "absolute". Note that
        // "drive" can be empty here, which is fine.

        if (path(0) == '\\')
          (path drop 1, drive + "\\")
        else
          (path, drive + "\\")
    }
  }

  /** Posix pattern splitter for eglob(). See description for the
    * eglobPatternSplitter value, above.
    *
    * @param pattern  the pattern to split
    *
    * @return a (directory, subpattern) tuple
    */
  private def splitPosixEglobPattern(pattern: String): (String, String) = {
    if (pattern.length == 0)
      (".", ".")

    else if (pattern(0) == fileSeparatorChar)
      (pattern drop 1, "/")

    else
      (pattern, ".")
  }

  /** Path normalization is operating system-specific. This value
    * holds the real path normalizer, determined once.
    */
  private lazy val doPathNormalizing = os match {
    case (Mac | Posix) => normalizePosixPath _
    case Windows       => normalizeWindowsPath _
    case _             => (path: String) => path
  }

  /** Shared between normalizeWindowsPath() and normalizePosixPath(),
    * this function normalizes the pieces of a path, handling embedded "..",
    * empty elements (from splitting when there are adjacent file separators),
    * etc.
    *
    * @param pieces  path components, with no separators
    *
    * @return sanitized list of path components
    */
  private def normalizePathPieces(pieces: List[String]): List[String] = {
    pieces match {
      case Nil =>
        Nil

      case "" :: tail =>
        normalizePathPieces(tail)

      case "." :: tail =>
        normalizePathPieces(tail)

      case a :: ".." :: tail =>
        normalizePathPieces(tail)

      case head :: tail =>
        List[String](head) ++ normalizePathPieces(tail)
    }
  }

  /** Native-to-universal path conversion is operating system-specific.
    * These values hold the real converters, determined once.
    */
  private lazy val makeUniversalPath: (String) => String = os match {
    case (Mac | Posix) =>
      (path: String) => path
    case Windows =>
      (path: String) => path.replace(fileSeparator, "/")
    case _ =>
      (path: String) => path
  }

  private lazy val makeNativePath: (String) => String = os match {
    case (Mac | Posix) =>
      (path: String) => path
    case Windows =>
      (path: String) => path.replace("/", fileSeparator)
    case _ =>
      (path: String) => path
  }

}
