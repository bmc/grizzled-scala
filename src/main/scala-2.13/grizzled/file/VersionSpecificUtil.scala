package grizzled.file

import java.io.File

/** Scala 2.13+ versions of certain `grizzled.file.util` functions. This
  * trait is mixed into `grizzled.file.util` when compiling against 2.13 and
  * newer. There's a corresponding version of this trait for pre-2.13 builds.
  */
trait VersionSpecificUtil {

  /** List a directory recursively, returning `File` objects for each file
    * (and subdirectory) found. This method does lazy evaluation, instead
    * of calculating everything up-front, as `walk()` does.
    *
    * The JDK's [[https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#walk-java.nio.file.Path-int-java.nio.file.FileVisitOption...- java.nio.file.Files.walk()]]
    * function provides a similar capability in JDK 8. Prior to JDK 8, you can
    * also use  [[https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#walkFileTree(java.nio.file.Path,%20java.util.Set,%20int,%20java.nio.file.FileVisitor) java.nio.file.Files.walkFileTree()]]
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
}
