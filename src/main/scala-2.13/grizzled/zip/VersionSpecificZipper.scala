package grizzled.zip

import grizzled.file.{util => fileutil}

import java.io.File

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** Scala 2.13+ versions of certain `grizzled.zip.Zipper` functions. This
  * trait is mixed into `grizzled.zip.Zipper` when compiling against Scala 2.13
  * and newer. There's a corresponding version of this trait for older versions
  * of Scala.
  */
trait VersionSpecificZipper {
  self: Zipper =>

  protected[zip] def addRecursively(dir: File,
                                    strip: Option[String],
                                    flatten: Boolean,
                                    wildcard: Option[String]): Try[Zipper] = {

    import LazyList.#::

    @tailrec
    def addNext(files: LazyList[File], currentZipper: Zipper): Try[Zipper] = {

      def wildcardMatch(f: File): Boolean = {
        wildcard.forall(pat => fileutil.fnmatch(f.getName, pat))
      }

      files match {
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
