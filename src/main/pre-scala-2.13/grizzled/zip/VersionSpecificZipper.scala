package grizzled.zip

import grizzled.file.{util => fileutil}

import java.io.File

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** Pre-Scala 2.13 versions of certain `grizzled.zip.Zipper` functions. This
  * trait is mixed into `grizzled.zip.Zipper` when compiling against versions
  * of Scala prior to 2.13. There's a corresponding version of this trait for
  * 2.13 and newer.
  */
trait VersionSpecificZipper {
  self: Zipper =>

  /** Utility method to create a new `Zipper` from the current `Zipper`, by
    * adding all files in a directory to the current `Zipper`. This method
    * is used by `Zipper.addDirectory()`. Under the covers, it uses `Stream`
    * prior to Scala 2.13 and `LazyList` in 2.13 and newer.
    *
    * @param dir       the directory, which must exist
    * @param strip     optional leading path to strip. If not specified,
    *                  the full path to each file (minus file system root)
    *                  is used.
    * @param flatten   whether or not to flatten the entries. Note that a
    *                  `true` value can cause errors if files in different
    *                  directories have the same name.
    * @param wildcard  optional wildcard to match files against. If `None`,
    *                  all files found are added. This is a simple glob
    *                  pattern, acceptable to [[grizzled.file.util.fnmatch]].
    */
  protected[zip] def addRecursively(dir: File,
                                    strip: Option[String],
                                    flatten: Boolean,
                                    wildcard: Option[String]): Try[Zipper] = {

    @tailrec
    def addNext(stream: Stream[File], currentZipper: Zipper): Try[Zipper] = {

      def wildcardMatch(f: File): Boolean = {
        wildcard.forall(pat => fileutil.fnmatch(f.getName, pat))
      }

      stream match {
        case s if s.isEmpty =>
          Success(currentZipper)
        case head #:: tail if head.isDirectory =>
          addNext(tail, currentZipper)
        case head #:: tail if ! wildcardMatch(head) =>
          addNext(tail, currentZipper)
        case head #:: tail =>
          val f = head       // the next file or directory (File)
        val path = f.getPath // its path (String)
        val t = if (flatten)
          currentZipper.addFile(f, flatten = true)
        else {
          strip
            .map { p =>
              if (path.startsWith(p))
                currentZipper.addFile(f, path.substring(p.length))
              else
                currentZipper.addFile(f)
            }
            .getOrElse(currentZipper.addFile(f))
        }

          t match {
            case Failure(ex) => Failure(ex)
            case Success(z)  => addNext(tail, z)
          }
      }
    }

    addNext(fileutil.listRecursively(dir), this)
  }

}
