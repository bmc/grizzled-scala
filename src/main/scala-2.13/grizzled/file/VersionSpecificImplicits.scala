package grizzled.file

import java.io.File

/** Scala 2.13+ versions of certain `grizzled.file.Implicits` functions. This
  * trait is mixed into `grizzled.file.Implicits` when compiling against Scala
  * 2.13 and newer. There's a corresponding version of this trait for older
  * versions of Scala.
  */
trait VersionSpecificImplicits {
  self: Implicits.GrizzledFile =>

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
    * @return a stream of `File` objects for everything under
    *         the directory.
    */
  def listRecursively(topdown: Boolean = true): LazyList[File] =
    util.listRecursively(this.file, topdown)
}
