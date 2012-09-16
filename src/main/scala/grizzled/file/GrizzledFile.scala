/*
  ---------------------------------------------------------------------------
  This software is released under a BSD license, adapted from
  http://opensource.org/licenses/bsd-license.php

  Copyright (c) 2009-2010, Brian M. Clapper
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  * Neither the names "clapper.org", "Grizzled Scala Library", nor the
    names of its contributors may be used to endorse or promote products
    derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  ---------------------------------------------------------------------------
*/

package grizzled.file

import grizzled.generator._

import java.io.File

/**
 * A wrapper for `java.io.File` that provides additional methods.
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
final class GrizzledFile(val file: File) {
  /**
   * Get the directory name of the file.
   *
   * @return the directory portion, as a `File`.
   */
  def dirname = new File(util.dirname(file.getPath))

  /**
   * Get the basename (file name only) part of a path.
   *
   * @return the file name portion, as a `File`
   */
  def basename = new File(util.basename(file.getPath))

  /**
   * Get the path of this file, relative to some other file.
   *
   * @param relativeTo  the other file
   *
   * @return the path of this file, relative to the other file.
   */
  def relativePath(relativeTo: File): String =
    util.relativePath(this, relativeTo)

  /**
   * Split the file's path into directory (dirname) and file (basename)
   * components. Analogous to Python's `os.path.pathsplit()` function.
   *
   * @return a (dirname, basename) tuple of `File` objects.
   */
  def dirnameBasename: (File, File) = {
    val tuple = util.dirnameBasename(file.getPath)
    (new File(tuple._1), new File(tuple._2))
  }

  /**
   * Recursively remove the directory specified by this object. This
   * method is conceptually equivalent to `rm -r` on a Unix system.
   */
  def deleteRecursively(): Unit = util.deleteTree(file)

  /**
   * Split this file's pathname into the directory name, basename, and
   * extension pieces.
   *
   * @param pathname the pathname
   *
   * @return a 3-tuple of (dirname, basename, extension)
   */
  def dirnameBasenameExtension: (File, String, String) = {
    val (dir, base, ext) = util.dirnameBasenameExtension(file.getPath)
    (new File(dir), base, ext)
  }

  /**
   * Split this file's path into its constituent components. If the path
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
   * @param path    the path
   *
   * @return the component pieces.
   */
  def split: List[String] = util.splitPath(file.getPath)

  /**
   * Similar to the Unix <i>touch</i> command, this function:
   *
   * <ul>
   *   <li>updates the access and modification time for the path
   *       represented by this object
   *   <li>creates the path (as a file), if it does not exist
   * </ul>
   *
   * If the file corresponds to an existing directory, this method
   * will throw an exception.
   *
   * @param time   Set the last-modified time to this time, or to the current
   *               time if this parameter is negative.
   *
   * @throws IOException on error
   */
  def touch(time: Long = -1): Unit = util.touch(file.getPath, time)

  /**
   * Directory tree lister, adapted from Python's `os.walk()` function.
   * NOTE: This function generates the entire directory tree in memory,
   * before returning. If you want a lazy generator, with optional filtering,
   * use the `listRecursively()` method.
   *
   * For each directory in the directory tree rooted at this object
   * (including the directory itself, but excluding '.' and '..'), yields
   * a 3-tuple.
   *
   * {{{
   * dirpath, dirnames, filenames
   * }}}
   *
   * `dirpath` is a string, the path to the directory. `dirnames`is a
   * list of the names of the subdirectories in `dirpath` (excluding '.'
   * and '..'). `filenames` is a list of the names of the non-directory
   * files in `dirpath`. Note that the names in the lists are just names,
   * with no path components. To get a full path (which begins with this
   * directory) to a file or directory in `dirpath`, use `dirpath +
   * java.io.fileSeparator + name`, or use
   * `grizzled.file.util.joinPath()`.
   *
   * If `topdown` is `true`, the triplet for a directory is generated
   * before the triplets for any of its subdirectories (directories are
   * generated top down). If `topdown` is `false`, the triplet for a
   * directory is generated after the triples for all of its
   * subdirectories (directories are generated bottom up).
   *
   * **WARNING!** This method does not grok symbolic links!
   *
   * @param topdown `true` to do a top-down traversal, `false` otherwise.
   *
   * @return List of triplets, as described above.
   */
  def walk(topdown: Boolean = true): List[(String, List[String], List[String])] =
    util.walk(file.getPath, topdown)

  /**
   * List a directory recursively, returning `File` objects for each file
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
   * @return a generator (iterator) of `File` objects for everything under
   *         the directory.
   */
  def listRecursively(topdown: Boolean = true): Iterator[File] =
    util.listRecursively(file, topdown)

  /**
   * Determine whether a directory is empty. Only meaningful for a directory.
   *
   * @return true if the directory is empty, false if not
   */
  def isEmpty: Boolean = {
    assert (file.isDirectory)
    file.listFiles.length == 0
  }

  /**
   * Copy the file to a target directory or file.
   *
   * @param target  path to the target file or directory
   *
   * @return the target file
   */
  def copyTo(target: String): File = copyTo(new File(target))

  /**
   * Copy the file to a target directory or file.
   *
   * @param target  path to the target file or directory
   *
   * @return the target file
   */
  def copyTo(target: File): File = util.copyFile(file, target)
}

/**
 * Companion object for `GrizzledFile`. To get implicit functions that
 * define automatic conversions between `GrizzledFile` and `java.io.File`,
 * import this module:
 *
 * {{{
 * import grizzled.file.GrizzledFile._
 * }}}
 */
object GrizzledFile {
  import scala.language.implicitConversions

  implicit def javaIoFileToGrizzledFile(f: File): GrizzledFile =
    new GrizzledFile(f)

  implicit def grizzledFileToJavaIoFile(gf: GrizzledFile): File = gf.file
}
