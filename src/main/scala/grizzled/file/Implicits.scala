package grizzled.file

import java.io.File

import scala.util.{Failure, Success, Try}

/** Enrichment classes for `File` objects and the like.
  */
object Implicits {
  import scala.language.implicitConversions

  import grizzled.ScalaCompat._
  import grizzled.file.{util => fileutil}

  /** A wrapper for `java.io.File` that provides additional methods.
    * By importing the implicit conversion functions, you can use the methods
    * in this class transparently from a `java.io.File` object.
    *
    * {{{
    * import grizzled.file.GrizzledFile._
    *
    * val file = new File("/tmp/foo/bar")
    * println(file.split) // prints: List(/tmp, foo, bar)
    * }}}
    */
  implicit class GrizzledFile(val file: File) {

    /** A version of `java.io.File.exists` that returns a `Try`, this
      * method tests the existence of the file.
      *
      * @return `Success(true)` if the file exists, and
      *         `Failure(FileDoesNotExistException)` if it does not.
      */
    def pathExists: Try[Boolean] = {
      if (file.exists)
        Success(true)
      else
        Failure(new FileDoesNotExistException(s""""${file.toString}" does not exist."""))
    }

    /** Get the directory name of the file.
      *
      * @return the directory portion, as a `File`.
      */
    def dirname: File = new File(fileutil.dirname(file.getPath))

    /** Get the basename (file name only) part of a path.
      *
      * @return the file name portion, as a `File`
      */
    def basename: File = new File(fileutil.basename(file.getPath))

    /** Get the path of this file, relative to some other file.
      *
      * @param relativeTo  the other file
      *
      * @return the path of this file, relative to the other file.
      */
    def relativePath(relativeTo: File): String =
      fileutil.relativePath(this, relativeTo)

    /** Split the file's path into directory (dirname) and file (basename)
      * components. Analogous to Python's `os.path.pathsplit()` function.
      *
      * @return a (dirname, basename) tuple of `File` objects.
      */
    def dirnameBasename: (File, File) = {
      val tuple = fileutil.dirnameBasename(file.getPath)
      (new File(tuple._1), new File(tuple._2))
    }

    /** Recursively remove the directory specified by this object. This
      * method is conceptually equivalent to `rm -r` on a Unix system.
      *
      * @return `Failure(exception)` or `Success(total)`, where `total` is the
      *         number of files and directories actually deleted.
      */
    def deleteRecursively(): Try[Int] = fileutil.deleteTree(file)

    /** Split this file's pathname into the directory name, basename, and
      * extension pieces.
      *
      * @return a 3-tuple of (dirname, basename, extension)
      */
    def dirnameBasenameExtension: (File, String, String) = {
      val (dir, base, ext) = fileutil.dirnameBasenameExtension(file.getPath)
      (new File(dir), base, ext)
    }

    /** Split this file's path into its constituent components. If the path
      * is absolute, the first piece will have a file separator in the
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
      * @return the component pieces.
      */
    def split: List[String] = fileutil.splitPath(file.getPath)

    /** Similar to the Unix ''touch'' command, this function:
      *
      *  - updates the access and modification time for the path
      *    represented by this object
      *  - creates the path (as a file), if it does not exist
      *
      * If the file corresponds to an existing directory, this method
      * will return an error.
      *
      * @param time   Set the last-modified time to this time, or to the current
      *               time if this parameter is negative.
      *
      * @return `Success(true)` on success, `Failure(exception)` on error.
      */
    def touch(time: Long = -1L): Try[Boolean] = {
      fileutil.touch(file.getPath, time)
    }

    /** Determine whether a directory is empty. Only meaningful for a directory.
      *
      * @return true if the directory is empty, false if not
      */
    def isEmpty: Boolean = {
      assert (file.isDirectory)
      file.listFiles.isEmpty
    }

    /** List a directory recursively, returning `File` objects for each file
      * (and subdirectory) found. This method does lazy evaluation, instead
      * of calculating everything up-front, as `walk()` does.
      *
      * If `topdown` is `true`, a directory is generated before the entries
      * for any of its subdirectories (directories are generated top down).
      * If `topdown` is `false`, a directory directory is generated after
      * the entries for all of its subdirectories (directories are generated
      * bottom up).
      *
      * @param topdown `true` to do a top-down traversal, `false` otherwise.
      *
      * @return a lazy sequence of `File` objects for everything under
      *         the directory. This sequence will be a `LazyList` (typedef'd)
      *         in Scala 2.13 or better, and a `Stream` in Scala 2.12 and older.
      */
    def listRecursively(topdown: Boolean = true): LazyList[File] =
      fileutil.listRecursively(this.file, topdown)

    /** Copy the file to a target directory or file.
      *
      * @param target  path to the target file or directory
      *
      * @return A `Success` containing the target file, or `Failure(exception)`
      */
    def copyTo(target: String): Try[File] = copyTo(new File(target))

    /** Copy the file to a target directory or file.
      *
      * @param target  path to the target file or directory
      *
      * @return A `Success` containing the target file, or `Failure(exception)`
      */
    def copyTo(target: File): Try[File] = fileutil.copyFile(file, target)
  }

  implicit def grizzledFileToJavaIoFile(gf: GrizzledFile): File = gf.file
}
