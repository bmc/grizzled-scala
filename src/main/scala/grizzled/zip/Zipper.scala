package grizzled.zip

import grizzled.file.{util => fileutil}
import grizzled.file.Implicits.GrizzledFile
import java.io._
import java.net.URL
import java.util.jar.{JarOutputStream, Manifest => JarManifest}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.annotation.tailrec
import scala.collection.Set
import scala.io.Source
import scala.util.{Failure, Success, Try}

/** ==Zipper: Write zip and jar files more easily==
  *
  * The `Zipper` class provides a convenient mechanism for writing zip and jar
  * files; it's a simplifying layer that sits on top of the existing Zip and
  * Jar classes provided by the JDK. A `Zipper` object behaves somewhat like an
  * immutable Scala collection, into which you can drop `File` objects,
  * `InputStream` objects, `Reader` objects, `Source` objects, URLs and
  * pathnames. When you call `writeZip` or `writeJar`, the objects in `Zipper`
  * are written to the actual underlying zip or jar file.
  *
  * A `Zipper` can either preserve pathnames or flatten the paths down to single
  * components. When preserving pathnames, a `Zipper` object converts absolute
  * paths to relative paths by stripping any leading "file system mount points."
  * On Unix-like systems, this means stripping the leading "/"; on Windows, it
  * means stripping any leading drive letter and the leading "\". (See
  * java.io.File.listRoots() for more information.) For instance, if you're not
  * flattening pathnames, and you add `C:\Temp\hello.txt` to a `Zipper` on
  * Windows, the `Zipper` will strip the `C:\`, adding `Temp/hello.txt`. to the
  * zip or jar file. If you're on a Unix-like system, including Mac OS X, and
  * you add `/tmp/foo/bar.txt`, the `Zipper` will add `tmp/foo/bar.txt` to the
  * file.
  *
  * ==Directories==
  *
  * You can explicitly add directory entries to a `Zipper`, using
  * `addDirectory()`. When you're not flattening entries, a `Zipper` object will
  * also ensure that any intermediate directories in a pathname are created in
  * the zip file. For instance, if you add file `/tmp/foo/bar/baz.txt` to a
  * `Zipper`, without flattening it, the `Zipper` will create the following
  * entries in the underlying zip file:
  *
  *  - `tmp` (directory)
  *  - `tmp/foo` (directory)
  *  - `tmp/foo/bar` (directory)
  *  - `tmp/foo/bar/baz.txt` (the entry)
  *
  * If you use the JDK's zip or jar classes directly, you have to create those
  * intermediate directory entries yourself. In addition, you have to be careful
  * not to create a directory more than once; doing so will cause an error.
  * `Zipper` automatically creating unique intermediate directories for you.
  *
  * ==Constructing a Zipper object==
  *
  * The class constructor is private; use the companion object's `apply()`
  * functions to instantiate `Zipper` objects.
  *
  * ==Using a Zipper object==
  *
  * The `add()` methods all return `Try` objects, and they do not modify
  * the original `Zipper` object. On success, they return a `Success` object
  * that contains a ''new'' `Zipper`.
  *
  * Because the `add()` methods return `Try`, they are unsuitable for use
  * in traditional "builder" patterns. For instance, the following will ''not''
  * work:
  *
  * {{{
  * // Will NOT work
  * val zipper = Zipper()
  * zipper.add("/tmp/foo/bar.txt").add("/tmp/baz.txt")
  * }}}
  *
  * There are other patterns you can use, however. Since `Try` is monadic, a
  * `for` comprehension works nicely:
  *
  * {{{
  * val zipper = Zipper()
  * val newZipper = for { z1 <- zipper.add("/tmp/foo/bar.txt")
  *                       z2 <- z1.add("/tmp/baz.txt")
  *                       z3 <- z2.add("hello.txt") }
  *                 yield z3
  * // newZipper is a Try[Zipper]
  * }}}
  *
  * If you're trying to add a collection of objects, a `for` comprehension
  * can be problematic. If you're not averse to using a local `var`, you
  * can just use a traditional imperative loop:
  *
  * {{{
  * val zipper = Zipper()
  * var z = zipper
  * val paths: List[String] = ...
  *
  * for (zipPath <- paths) {
  *   val t = z.add(zipPath)
  *   z = t.get // will throw an exception if the add failed
  * }
  * }}}
  *
  * You can also avoid a `var` using `foldLeft()`, though you still have to
  * contend with a thrown exception. (You can always wrap the code in a `Try`.)
  *
  * {{{
  * val zipper = Zipper()
  * val paths: List[String] = ...
  * paths.foldLeft(zipper) { case (z, zipPath) =>
  *   z.add(zipPath).get // throws an exception if the add fails
  * }
  * }}}
  *
  * Finally, to avoid the exception ''and'' the `var`, use tail-recursion:
  *
  * {{{
  * import scala.annnotation.tailrec
  * import scala.util.{Failure, Success, Try}
  *
  * @tailrec
  * def addNext(paths: List[String], currentZipper: Zipper): Try[Zipper] = {
  *   paths match {
  *     case Nil => Success(currentZipper)
  *     case zipPath :: rest =>
  *       // Can't use currentZipper.add(zipPath).map(), because the recursion
  *       // will then be invoked within the lambda, violating tail-recursion.
  *       currentZipper.add(zipPath) match {
  *         case Failure(ex) => Failure(ex)
  *         case Success(z)  => addNext(rest, z)
  *       }
  *   }
  * }
  *
  * val paths: List[String] = ...
  * val zipper = addNext(paths, Zipper())
  * }}}
  *
  * ==Notes==
  *
  * A `Zipper` is not a true Scala collection. It does not support extensively
  * querying its contents, looping over them, or transforming them. It is simply a
  * container to be filled and then written.
  *
  * The `Zipper` class currently provides no support for storing uncompressed
  * (i.e., fully inflated) entries. All data stored in the underlying zip is
  * compressed, even though the JDK-supplied zip classes support both compressed
  * and uncompressed entries. If necessary, the `Zipper` class can be extended
  * to support storing uncompressed data.
  **/
class Zipper private(items:           Map[String, ZipSource],
                     bareDirectories: Set[String]) {

  /** Add a file to the `Zipper`. The zipPath in the resulting zip or jar file
    * will be the zipPath (if it's relative) or the zipPath with the file system root
    * removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param path zipPath to the file to add
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(path: String): Try[Zipper] = add(path, flatten = false)

  /** Add a file to the `Zipper`. The entry in the zip file will be the
    * base name of the file, if `flatten` is specified. Otherwise, it'll
    * be the zipPath itself (if the zipPath is relative) or the zipPath with the file
    * system root removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param path     zipPath to the file to add
    * @param flatten  whether or not to flatten the zipPath in the zip file
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(path: String, flatten: Boolean): Try[Zipper] = {
    addItem(FileSource(new File(path)), path, flatten)
  }

  /** Add a file to the `Zipper`, specifying the zip file entry name explicitly.
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param path     zipPath to the file to add
    * @param zipPath  the zipPath of the entry in the zip or jar file. Any file
    *                 system root will be stripped.
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(path: String, zipPath: String): Try[Zipper] = {
    addItem(FileSource(new File(path)), zipPath, flatten = false)
  }

  /** Add a file to the `Zipper`. The zipPath in the resulting zip or jar file
    * will be the zipPath (if it's relative) or the zipPath with the file system root
    * removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param f  the `File` to be added
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(f: File): Try[Zipper] = add(f, flatten = false)


  /** Add a file to the `Zipper`. The entry in the zip file will be the
    * base name of the file, if `flatten` is specified. Otherwise, it'll
    * be the zipPath itself (if the zipPath is relative) or the zipPath with the file
    * system root removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param f        the `File` to be added
    * @param flatten  whether or not to flatten the zipPath in the zip file
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(f: File, flatten: Boolean): Try[Zipper] = add(f.getPath, flatten)

  /** Add a file to the `Zipper`, specifying the zip file entry name explicitly.
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param f        the `File` to be added
    * @param zipPath  the zipPath of the entry in the zip or jar file. Any file
    *                 system root will be stripped.
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(f: File, zipPath: String): Try[Zipper] = add(f.getPath, zipPath)

  /** Add a URL to the `Zipper`. The zipPath in the zip file will be taken from
    * the zipPath component of the URL. That means the URL ''must'' have a
    * file name component. For instance, if you add the URL
    * `http://www.example.com/`, you'll get an error, because the zipPath
    * component is "/", and the corresponding relative zipPath is "". In other
    * words, `Zipper` does ''not'' add `index.html` for you automatically. A
    * URL like `http://www.example.com/index.html` will work fine, resulting
    * in `index.html` being added to the resulting zip file. Similarly, using
    * this method to add `http://www.example.com/music/My-Song.mp3` will write
    * `music/My-Song.mp3` to the zip or jar file.
    *
    * '''Note''': The URL is not validated (i.e., no connection is made) until
    * you call `writeZip()` or `writeJar()`.
    *
    * @param url  the URL to the resource to be added
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(url: URL): Try[Zipper] = add(url, flatten = false)

  /** Add a URL to the `Zipper`. The zipPath in the zip file will be taken from
    * the zipPath component of the URL, and all directories will be stripped from
    * the zipPath. That means the URL ''must'' have a file name component. For
    * instance, if you add the URL `http://www.example.com/`, you'll get an
    * error, because the zipPath component is "/", and the corresponding relative
    * zipPath is "". In other words, `Zipper` does ''not'' add `index.html` for you
    * automatically. A URL like `http://www.example.com/index.html` will work
    * fine, resulting in `index.html` being added to the resulting zip file.
    * Using this method to add
    * `http://www.example.com/music/My-Song.mp3` will write `music/My-Song.mp3`
    * to the zip or jar file, if `flatten` is `false`; if `flatten` is `true`,
    * `My-Song.mp3` will be written.
    *
    * '''Note''': The URL is not validated (i.e., no connection is made) until
    * you call `writeZip()` or `writeJar()`.
    *
    * @param url  the URL to the resource to be added
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(url: URL, flatten: Boolean): Try[Zipper] = {
    addItem(item          = URLSource(url),
            path          = url.getPath,
            flatten       = flatten,
            forceRoot     = Some("/"))
  }

  /** Add a `scala.io.Source` to the `Zipper`, using the specified path in
    * the zip file.
    *
    * '''Warning''': A `Source` represents an open resource (e.g., an open
    * file descriptor). Those resources are held open until you call
    * `writeZip()` or `writeJar()`. If you add too many `Source` objects
    * (or `Reader` or `InputStream` objects) to a `Zipper`, you could
    * theoretically, run out of open file descriptors.
    *
    * @param source   the `Source` to add
    * @param zipPath  the path to use within the zip file. Any file system
    *                 root is removed from this path.
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(source: Source, zipPath: String): Try[Zipper] = {
    addItem(SourceSource(source), zipPath, flatten = false)
  }

  /** Add a `scala.io.SOurce` to the `Zipper`, using the specified path in
    * the zip file. If `flatten` is specified, all directories will be removed
    * from the zip path; otherwise, it will be used as-is, with any file system
    * root removed.
    *
    * '''Warning''': A `Source` represents an open resource (e.g., an open
    * file descriptor). Those resources are held open until you call
    * `writeZip()` or `writeJar()`. If you add too many `Source` objects
    * (or `Reader` or `InputStream` objects) to a `Zipper`, you could
    * theoretically, run out of open file descriptors.
    *
    * @param source   the `Source` to add
    * @param zipPath  the path to use within the zip file. Any file system
    *                 root is removed from this path.
    * @param flatten  whether or not to flatten the zip path
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(source: Source, zipPath: String, flatten: Boolean): Try[Zipper] = {
    addItem(SourceSource(source), zipPath, flatten)
  }

  /** Add an `InputStream` to the `Zipper`, using the specified path in
    * the zip file.
    *
    * '''Warning''': An `InputStream` represents an open resource (e.g., an open
    * file descriptor). Those resources are held open until you call
    * `writeZip()` or `writeJar()`. If you add too many `InputStream` objects
    * (or `Reader` or `Source` objects) to a `Zipper`, you could theoretically,
    * run out of open file descriptors.
    *
    * @param inputStream  the `InputStream` to add
    * @param zipPath      the path to use within the zip file. Any file system
    *                     root is removed from this path.
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(inputStream: InputStream, zipPath: String): Try[Zipper] = {
    addItem(InputStreamSource(inputStream), zipPath, flatten = false)
  }

  /** Add an `InputStream` to the `Zipper`, using the specified path in
    * the zip file. If `flatten` is specified, all directories will be removed
    * from the zip path; otherwise, it will be used as-is, with any file system
    * root removed.
    *
    * '''Warning''': An `InputStream` represents an open resource (e.g., an open
    * file descriptor). Those resources are held open until you call
    * `writeZip()` or `writeJar()`. If you add too many `InputStream` objects
    * (or `Reader` or `Source` objects) to a `Zipper`, you could theoretically,
    * run out of open file descriptors.
    *
    * @param inputStream  the `InputStream` to add
    * @param zipPath      the path to use within the zip file. Any file system
    *                     root is removed from this path.
    * @param flatten      whether or not to flatten the zip path
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(inputStream: InputStream,
          zipPath:     String,
          flatten:     Boolean): Try[Zipper] = {
    addItem(InputStreamSource(inputStream), zipPath, flatten)
  }

  /** Add a `Reader` to the `Zipper`, using the specified path in the zip file.
    *
    * '''Warning''': A `Reader` represents an open resource (e.g., an open file
    * descriptor). Those resources are held open until you call `writeZip()` or
    * `writeJar()`. If you add too many `InputStream` objects (or `InputStream`
    * or `Source` objects) to a `Zipper`, you could theoretically, run out of
    * open file descriptors.
    *
    * @param reader   the `Reader` to add
    * @param zipPath  the path to use within the zip file. Any file system
    *                 root is removed from this path.
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(reader: Reader, zipPath: String): Try[Zipper] = {
    addItem(ReaderSource(reader), zipPath, flatten = false)
  }

  /** Add a `Reader` to the `Zipper`, using the specified path in the zip file.
    * If `flatten` is specified, all directories will be removed from the zip
    * path; otherwise, it will be used as-is, with any file system root removed.
    *
    * '''Warning''': A `Reader` represents an open resource (e.g., an open file
    * descriptor). Those resources are held open until you call `writeZip()` or
    * `writeJar()`. If you add too many `InputStream` objects (or `InputStream`
    * or `Source` objects) to a `Zipper`, you could theoretically, run out of
    * open file descriptors.
    *
    * @param reader   the `Reader` to add
    * @param zipPath  the path to use within the zip file. Any file system
    *                 root is removed from this path.
    * @param flatten  whether or not to flatten the zip path
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def add(reader: Reader, zipPath: String, flatten: Boolean): Try[Zipper] = {
    addItem(ReaderSource(reader), zipPath, flatten)
  }

  /** Add a directory entry to the `Zipper`. The path should be in "/" form,
    * even on Windows, since zip and jar files always use "/". Any leading
    * "/" will be removed, converting it to a relative path.
    *
    * @param path the path of the directory entry to add
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addDirectory(path: String): Try[Zipper] = {
    def ensureNotThere(path: String): Try[Unit] = {
      if (bareDirectories contains path) {
        Failure(new Exception(s"""Zipper already contains directory "$path""""))
      }
      else {
        Success(())
      }
    }

    for { path <- stripRoot(path, forceRoot = Some("/"))
          _    <- ensureNotThere(path) }
      yield new Zipper(items = items, bareDirectories = bareDirectories + path)
  }

  /** The unique paths in the `Zipper`. The directory entries will be
    * suffixed with "/". Note that intermediate directory entries will ''not''
    * be represented in this list. Only the paths that have been explicitly
    * added are represented.
    */
  val paths = {
    bareDirectories.map { _ + "/" } ++ items.keySet
  }

  /** Write the contents of this `Zipper` to a jar file. The jar file will
    * not have a jar manifest. You can call this method more than once.
    *
    * '''Warning''': While you can call this method multiple times (to write
    * a single `Zipper` to multiple zip files, for instance), some entry
    * sources cannot be read multiple times. For instance, `Zipper` does
    * not attempt to rewind `Reader`, `InputStream` or `Source` objects, so
    * they cannot be read more than once; reusing a `Zipper` containing those
    * types of sources will result in an error.
    *
    * @param path the path to the jar file to write. If it exists, it will be
    *             overwritten
    *
    * @return A `Success` with a `File` of the written jar, on success. A
    *         `Failure` on error.
    */
  def writeJar(path: String): Try[File] = writeJar(new File(path), None)

  /** Write the contents of this `Zipper` to a jar file. The jar file will
    * not have a jar manifest. You can call this method more than once.
    *
    * '''Warning''': While you can call this method multiple times (to write
    * a single `Zipper` to multiple zip files, for instance), some entry
    * sources cannot be read multiple times. For instance, `Zipper` does
    * not attempt to rewind `Reader`, `InputStream` or `Source` objects, so
    * they cannot be read more than once; reusing a `Zipper` containing those
    * types of sources will result in an error.
    *
    * @param jarFile  the jar file to write. If it exists, it will be
    *                 overwritten.
    *
    * @return A `Success` containing the `jarFile` parameter, on success. A
    *         `Failure` on error.
    */
  def writeJar(jarFile: File): Try[File] = writeJar(jarFile, manifest = None)

  /** Write the contents of this `Zipper` to a jar file, with or without a jar
    * manifest. You can call this method more than once.
    *
    * '''Warning''': While you can call this method multiple times (to write
    * a single `Zipper` to multiple zip files, for instance), some entry
    * sources cannot be read multiple times. For instance, `Zipper` does
    * not attempt to rewind `Reader`, `InputStream` or `Source` objects, so
    * they cannot be read more than once; reusing a `Zipper` containing those
    * types of sources will result in an error.
    *
    * @param jarFile  the jar file to write. If it exists, it will be
    *                 overwritten.
    * @param manifest optional jar manifest
    *
    * @return A `Success` containing the `jarFile` parameter, on success. A
    *         `Failure` on error.
    */
  def writeJar(jarFile: File, manifest: Option[JarManifest]): Try[File] = {

    def makeJarOutputStream(m: Option[JarManifest]): Try[JarOutputStream] = {
      Try {
        m.map { man =>
          new JarOutputStream(new FileOutputStream(jarFile), man)
        }
        .getOrElse {
          new JarOutputStream(new FileOutputStream(jarFile))
        }
      }
    }

    for { jo <- makeJarOutputStream(manifest)
          _  <- writeZipOutputStream(jo)
          _  <- Try { jo.close() } }
    yield jarFile
  }

  /** Write the contents of this `Zipper` to a zip file. You can call this
    * method more than once.
    *
    * '''Warning''': While you can call this method multiple times (to write
    * a single `Zipper` to multiple zip files, for instance), some entry
    * sources cannot be read multiple times. For instance, `Zipper` does
    * not attempt to rewind `Reader`, `InputStream` or `Source` objects, so
    * they cannot be read more than once; reusing a `Zipper` containing those
    * types of sources will result in an error.
    *
    * @param path the path to the zip file to write. If it exists, it will be
    *             overwritten
    *
    * @return A `Success` with a `File` of the written zip, on success. A
    *         `Failure` on error.
    */
  def writeZip(path: String): Try[File] = writeZip(new File(path))

  /** Write the contents of this `Zipper` to a zip file. You can call this
    * method more than once.
    *
    * '''Warning''': While you can call this method multiple times (to write
    * a single `Zipper` to multiple zip files, for instance), some entry
    * sources cannot be read multiple times. For instance, `Zipper` does
    * not attempt to rewind `Reader`, `InputStream` or `Source` objects, so
    * they cannot be read more than once; reusing a `Zipper` containing those
    * types of sources will result in an error.
    *
    * @param zipFile  the zip file to write. If it exists, it will be
    *                 overwritten.
    *
    * @return A `Success` containing the `zipFile` parameter, on success. A
    *         `Failure` on error.
    */
  def writeZip(zipFile: File): Try[File] = {
    for { zo <- Try { new ZipOutputStream(new FileOutputStream(zipFile)) }
          _  <- writeZipOutputStream(zo)
          _  <- Try {zo.close()} }
    yield zipFile
  }

  // --------------------------------------------------------------------------
  // Private methods
  // --------------------------------------------------------------------------

  /** Utility method to write the contents of this Zipper to an open
    * ZipOutputStream. Since a JarOutputStream is a subclass of a
    * ZipOutputStream, this method will work for both.
    *
    * @param zo  the open ZipOutputStream
    *
    * @return `Success` or `Failure`
    */
  private def writeZipOutputStream(zo: ZipOutputStream): Try[Unit] = {

    // Make all directory entries in the zip file, explicit or intermediate
    // (implied).
    def makeDirectories(): Try[Unit] = {

      Try {
        // Zip files need explicit directories. The following transformation
        // extracts all the parent directory names, breaks them into their
        // individual directory pieces, weeds out the duplicates, and sorts by
        // number of components (so that top-level directories are created
        // first). So, for instance, given these entries:
        //
        // - foo/bar/one.txt
        // - foo/two.txt
        // - foo/bar/baz/three.txt
        //
        // this transformation will produce a list with:
        // - foo
        // - foo/bar
        // - foo/bar/baz
        val directories = (bareDirectories ++ items.keySet.map { s =>
          fileutil.dirname(s)
        })
        .toList

        val uniqueDirs = directories.map { path =>
          fileutil.splitPath(path)
        }
        .flatMap { components =>
          // This takes a component like "foo/bar/baz" and breaks it up into
          // "foo", "foo/bar", "foo/bar/baz"
          components match {
            case Nil      => Nil
            case c :: Nil => List(List(c))
            case c        => for { i <- c.indices } yield c.slice(0, i + 1)
          }
        }
        .toSet // weed out duplicates
        .filter { p =>
          // Get rid of paths with "." or ".."
          val first = p.headOption
          // Can't use Option.contains() as long as we're still compiling
          // against 2.10. It didn't show up until 2.11. exists() is a
          // reasonable substitute.
          /*
          ! (first.isEmpty || first.contains(".") || first.contains(".."))
          */
          ! (first.isEmpty || first.exists(_ == ".") || first.exists(_ == ".."))
        }
        .toSeq    // back to sequence
        .sortBy { pieces: List[String] =>
          // Sort by size
          pieces.size
        }
        .map(_.mkString("/") + "/") // reassemble

        // Now, use that information to create the directories. In a zip file,
        // there must be an entry for each intermediate directory. Thus, if
        // we're storing file "foo/bar/baz/nb.scala", we need to create the
        // following directory entries in the zip file, before adding the file
        // notebook itself:
        //
        // - foo
        // - foo/bar
        // - foo/bar/baz
        //
        for (dir <- uniqueDirs) {
          // Make the entire zipPath, but not the top-level directory.
          val entry = new ZipEntry(dir)
          zo.putNextEntry(entry)
          //zo.closeEntry()
        }
      }
    }

    // Add all the files to the zip or jar.
    def makeFiles(): Try[Int] = {
      val tries = for (i <- items.values.toList) yield {
        val entry = new ZipEntry(i.zipPath)
        i.source.copyToZip(i.zipPath, zo)
      }

      tries.filter(_.isFailure) match {
        case Nil => Try { zo.closeEntry() }.map(_ => items.size)
        case failures =>
          val messages: List[String] = failures.map { f: Try[Int] =>
            f.map {
              // should never get here
              _.toString
            }
            .recover {
              case e: Exception => e.getMessage
            }
            .get
          }

          Failure(new IOException(s"Errors occurred writing zip: $messages"))
      }
    }

    // Main logic

    for { _  <- makeDirectories()
          _  <- makeFiles() }
    yield ()
  }

  /** Add a wrapped item to the Zipper.
    *
    * @param item      the item to add, wrapped in an `ItemSource`
    * @param path      the zipPath for the item in the zip file
    * @param flatten   whether or not to flatten the zipPath
    * @param forceRoot if not None, bypass the file system root, using this one
    *                  instead. (Useful with URLs, which always use a root of
    *                  "/", regardless of the current operating system.)
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  private def addItem(item:      ItemSource,
                      path:      String,
                      flatten:   Boolean,
                      forceRoot: Option[String] = None): Try[Zipper] = {
    def fixPath(p: String): Try[String] = {
      val newPath = if (flatten) {
        Success(fileutil.basename(p))
      }
      else {
        stripRoot(p, forceRoot)
      }

      newPath.flatMap { p =>
        if (p.isEmpty)
          Failure(new Exception(s"""Cannot find a file name in "$p"."""))
        else
          Success(p)
      }
    }

    def ensureNotThere(path: String): Try[Unit] = {
      if (items.contains(path))
        Failure(new Exception(s"""Path "$path" is already in the Zipper."""))
      else
        Success(())
    }

    for { zipPath <- fixPath(path)
          _       <- ensureNotThere(zipPath) }
    yield new Zipper(items = items + (zipPath -> ZipSource(item, zipPath)),
                     bareDirectories = bareDirectories)
  }

  /** Utility function to strip the file system root (or the specified root,
    * forceRoot) from a path.
    *
    * @param path        the path
    * @param forceRoot   the root to strip. If None, the file system roots
    *                    are used.
    *
    * @return A `Success` with the new path, or a `Failure` on error.
    */
  private def stripRoot(path: String, forceRoot: Option[String]): Try[String] = {
    val f = new File(path)

    val strippedPath = if (! f.isAbsolute) {
      // Already relative. Use as is.
      Success(path)
    }
    else if (forceRoot.isDefined) {
      val root = forceRoot.get
      if (path startsWith root)
        Success(path.substring(root.length))
      else
        Failure(new Exception(s""""$path" does not start with root "$root"."""))
    }
    else {
      // Absolute. Strip any leading file system roots.

      val lcPath = path.toLowerCase
      val matchingRoot = File.listRoots().filter { root =>
        val rootPath = root.getPath
        lcPath startsWith rootPath.toLowerCase
      }

      if (matchingRoot.isEmpty) {
        Failure(new IllegalArgumentException(
          s"""Absolute zipPath "$path" does not match a file system root."""
        ))
      }
      else {
        Success(path.substring(matchingRoot.head.getPath.length))
      }
    }

    strippedPath.map { p =>
      // If this is Windows, convert any Windows file separators to Unix-style.
      // The zip protocol prefers forward-slashes.
      if (File.separatorChar == '/') p else p.replace(File.separatorChar, '/')
    }
  }
}

/** Companion object to the `Zipper` class. You can only instantiate `Zipper`
  * objects via this companion.
  */
object Zipper {

  /** Create a new, empty `Zipper`.
    *
    * @return the new `Zipper` object.
    */
  def apply(): Zipper = {
    new Zipper(Map.empty[String, ZipSource], Set.empty[String])
  }

  /** Create a new empty `Zipper` object and fill it with the specified files.
    * This function is a convenience method; you can always create an empty
    * `Zipper` and fill it yourself.
    *
    * @param paths    the list of path names to add to the `Zipper`. The
    *                 existence of these files isn't verified until the zip
    *                 or jar file is written.
    * @param flatten  whether or not to flatten the paths.
    *
    * @return A `Success` with the filled `Zipper`, or a `Failure` if one or
    *         more of the paths could not be added.
    */
  def apply(paths: Array[String], flatten: Boolean): Try[Zipper] = {
    val toAdd = paths.map { s =>
      val f = new File(s)
      if (flatten)
        (f, fileutil.basename(s))
      else
        (f, s)
    }

    apply(toAdd)
  }

  /** Create a new empty `Zipper` object and fill it with the specified files.
    * This function is a convenience method; you can always create an empty
    * `Zipper` and fill it yourself.
    *
    * @param paths    the list of (`File`, pathname) tuples to be added. In
    *                 each tuple, the `File` element is the resource to be read
    *                 and added; the pathname is the path to use in the zip
    *                 or jar file.
    *
    * @return A `Success` with the filled `Zipper`, or a `Failure` if one or
    *         more of the paths could not be added.
    */
  def apply(paths: Array[(File, String)]): Try[Zipper] = {

    @tailrec
    def addNext(lPaths: List[(File, String)], z: Zipper): Try[Zipper] = {
      lPaths match {
        case Nil => Success(z)
        case (f, p) :: rest =>
          // Match match, not map, to ensure tail-recursion.
          z.add(f, p) match {
            case Failure(ex) => Failure(ex)
            case Success(z2) => addNext(rest, z2)
          }
      }
    }

    addNext(paths.toList, Zipper())
  }
}

// ----------------------------------------------------------------------------
// Private classes
// ----------------------------------------------------------------------------

/** Base trait for an item held in the `Zipper`. Provides a unified API for
  * reading from whatever kind of item has been added.
  */
private[zip] sealed trait ItemSource {

  // Buffer size to use when reading.
  val BufSize = 16 * 1024

  /** Read from the underlying resource, passing a buffer at a time to
    * a specified consumer lambda. The consumer will be passed the buffer
    * of bytes and the number of bytes that were read into the buffer. (The
    * buffer might be larger than the number of bytes read.) The consumer
    * might be called multiple times.
    *
    * @param consumer the consumer lambda, which must process the buffer of
    *                 bytes, returning a `Success` of the number of bytes
    *                 processed, or a `Failure` on error.
    *
    * @return A `Success` of the total number of bytes read and passed to
    *         the consumer, or a `Failure` on error.
    */
  def read(consumer: (Array[Byte], Int) => Try[Int]): Try[Int]

  /** Utility function that uses the `read()` function to copy the contents of
    * this item source to a `ZipOutputStream`.
    *
    * @param path the path for the entry in the zip file
    * @param zo   the `ZipOutputStream` to which to write
    *
    * @return A `Success` of the total number of bytes written to the stream,
    *         or a `Failure` on error.
    */
  def copyToZip(path: String, zo: ZipOutputStream): Try[Int] = {
    def doCopy() = {
      read { case (bytes, n) =>
        Try { zo.write(bytes, 0, n) }.map { _ =>  n }
      }
    }

    val entry = new ZipEntry(path)
    for { _   <- Try { zo.putNextEntry(entry) }
          n   <- doCopy()
          _   <- Try { entry.setSize(n) } }
    yield n

  }
}

/** Trait to mix into ItemSource classes that have to read an underlying
  * `InputStream`
  */
trait InputStreamHelper {
  self: ItemSource =>

  /** Utility function to read an `InputStream` and copy it to a consumer.
    *
    * @param is       the input stream
    * @param consumer the consumer
    * @return
    */
  protected def readInputStream(is: InputStream)
                               (consumer: (Array[Byte], Int) => Try[Int]):
    Try[Int] = {

    val buf = new Array[Byte](BufSize)

    @tailrec
    def readNext(readSoFar: Int): Try[Int] = {
      // Can't use flatMap or map, since we want tail recursion.
      Try { is.read(buf, 0, buf.length) } match {
        case Failure(ex) => Failure(ex)

        case Success(n) if n <= 0 => Success(readSoFar)

        case Success(n) =>
          consumer(buf, n)
          readNext(readSoFar + n)
      }
    }

    readNext(0)
  }
}

/** An `ItemSource` that contains and knows how to read from a URL.
  *
  * @param url the URL
  */
private[zip] case class URLSource(url: URL) extends ItemSource
                                            with InputStreamHelper {
  def read(consumer: (Array[Byte], Int) => Try[Int]) = {
    for { is <- Try { url.openStream() }
          n  <- readInputStream(is)(consumer) }
    yield n
  }
}

/** An `ItemSource` that contains and knows how to read from a `File` object.
  *
  * @param file the `File`
  */
private[zip] case class FileSource(file: File) extends ItemSource
                                               with InputStreamHelper {
  def read(consumer: (Array[Byte], Int) => Try[Int]) = {
    for { is <- Try { new FileInputStream(file) }
          n  <- readInputStream(is)(consumer)  }
    yield n
  }
}

/** An `ItemSource` that contains and knows how to read from an `InputStream`.
  *
  * @param is the input stream
  */
private[zip] case class InputStreamSource(is: InputStream)
  extends ItemSource
  with InputStreamHelper {

  def read(consumer: (Array[Byte], Int) => Try[Int]) = {
    readInputStream(is)(consumer)
  }
}

/** An `ItemSource` that contains and knows how to read from an `Reader`.
  *
  * @param r  the reader
  */
private[zip] case class ReaderSource(r: Reader) extends ItemSource {
  def read(consumer: (Array[Byte], Int) => Try[Int]) = {
    val buf = new Array[Char](BufSize)

    @tailrec
    def readNext(readSoFar: Int): Try[Int] = {

      // Can't use flatMap or map, since we want tail recursion.
      Try { r.read(buf, 0, buf.length) } match {
        case Failure(ex) => Failure(ex)

        case Success(n) if n == -1 => Success(readSoFar)

        case Success(n) =>
          consumer(buf.map(_.toByte), n)
          readNext(readSoFar + n)
      }
    }

    readNext(0)
  }
}

/** An `ItemSource` that contains and knows how to read from a
  * `scala.io.Source`
  *
  * @param source  the source
  */
private[zip] case class SourceSource(source: Source) extends ItemSource {
  def read(consumer: (Array[Byte], Int) => Try[Int]) = {

    @tailrec
    def readNext(readSoFar: Int): Try[Int] = {
      val buf = source.take(BufSize).map(_.toByte).toArray
      buf.length match {
        case 0 => Success(readSoFar)
        case n =>
          consumer(buf, n)
          readNext(readSoFar + n)
      }
    }

    readNext(0)
  }
}

/** The class for items that are added to a `Zipper`.
  *
  * @param source   the `ItemSource` container for the resource to be read
  * @param zipPath  the path in the zip file
  */
private[zip] case class ZipSource(source: ItemSource, zipPath: String)
