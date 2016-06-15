package grizzled.zip

import grizzled.file.Implicits.GrizzledFile
import grizzled.file.{util => fileutil}

import java.io._
import java.net.{URL => JavaURL}
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
  * `addZipDirectory()`. When you're not flattening entries, a `Zipper` object will
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
  * The `addFile()` methods all return `Try` objects, and they do not modify
  * the original `Zipper` object. On success, they return a `Success` object
  * that contains a ''new'' `Zipper`.
  *
  * Because the `addFile()` methods return `Try`, they are unsuitable for use
  * in traditional "builder" patterns. For instance, the following will ''not''
  * work:
  *
  * {{{
  * // Will NOT work
  * val zipper = Zipper()
  * zipper.addFile("/tmp/foo/bar.txt").addFile("/tmp/baz.txt")
  * }}}
  *
  * There are other patterns you can use, however. Since `Try` is monadic, a
  * `for` comprehension works nicely:
  *
  * {{{
  * val zipper = Zipper()
  * val newZipper = for { z1 <- zipper.addFile("/tmp/foo/bar.txt")
  *                       z2 <- z1.addFile("/tmp/baz.txt")
  *                       z3 <- z2.addFile("hello.txt") }
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
  * for (path <- paths) {
  *   val t = z.addFile(path)
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
  * paths.foldLeft(zipper) { case (z, path) =>
  *   z.addFile(path).get // throws an exception if the add fails
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
  *     case path :: rest =>
  *       // Can't use currentZipper.addFile(path).map(), because the recursion
  *       // will then be invoked within the lambda, violating tail-recursion.
  *       currentZipper.addFile(path) match {
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
class Zipper private(private val items:           Map[String, ZipSource],
                     private val bareDirectories: Set[String],
                             val comment:         Option[String] = None) {

  /** Add a file to the `Zipper`. The path in the resulting zip or jar file
    * will be the path (if it's relative) or the path with the file system root
    * removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param path path to the file to add
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addFile(path: String): Try[Zipper] = addFile(path, flatten = false)

  /** Add a file to the `Zipper`. The entry in the zip file will be the
    * base name of the file, if `flatten` is specified. Otherwise, it'll
    * be the path itself (if the path is relative) or the path with the file
    * system root removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param path     path to the file to add
    * @param flatten  whether or not to flatten the path in the zip file
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addFile(path: String, flatten: Boolean): Try[Zipper] = {
    addItem(FileSource(new File(path)), path, flatten)
  }

  /** Add a file to the `Zipper`, specifying the zip file entry name explicitly.
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param path     path to the file to add
    * @param zipPath  the path of the entry in the zip or jar file. Any file
    *                 system root will be stripped.
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addFile(path: String, zipPath: String): Try[Zipper] = {
    addItem(FileSource(new File(path)), zipPath, flatten = false)
  }

  /** Add a file to the `Zipper`. The path in the resulting zip or jar file
    * will be the path (if it's relative) or the path with the file system root
    * removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param f  the `File` to be added
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addFile(f: File): Try[Zipper] = addFile(f, flatten = false)

  /** Add a file to the `Zipper`. The entry in the zip file will be the
    * base name of the file, if `flatten` is specified. Otherwise, it'll
    * be the path itself (if the path is relative) or the path with the file
    * system root removed (if it's absolute).
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param f        the `File` to be added
    * @param flatten  whether or not to flatten the path in the zip file
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addFile(f: File, flatten: Boolean): Try[Zipper] =
    addFile(f.getPath, flatten)

  /** Add a file to the `Zipper`, specifying the zip file entry name explicitly.
    *
    * '''Note''': The existence or non-existence of the file isn't checked
    * until you call `writeZip()` or `writeJar()`.
    *
    * @param f        the `File` to be added
    * @param zipPath  the path of the entry in the zip or jar file. Any file
    *                 system root will be stripped.
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addFile(f: File, zipPath: String): Try[Zipper] =
    addFile(f.getPath, zipPath)

  /** Add a `java.net.URL` to the `Zipper`. The path in the zip file will be
    * taken from the path component of the URL. That means the URL ''must''
    * have a file name component. For instance, if you add the URL
    * `http://www.example.com/`, you'll get an error, because the path
    * component is "/", and the corresponding relative path is "". In other
    * words, `Zipper` does ''not'' add `index.html` for you automatically.
    * A URL like `http://www.example.com/index.html` will work fine, resulting
    * in `index.html` being added to the resulting zip file. Similarly, using
    * this method to add `http://www.example.com/music/My-Song.mp3` will
    * write `music/My-Song.mp3` to the zip or jar file.
    *
    * '''Note''': The URL is not validated (i.e., no connection is made) until
    * you call `writeZip()` or `writeJar()`.
    *
    * @param url     the URL to the resource to be added
    * @param zipPath the path within the zip file for the entry
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addURL(url: JavaURL, zipPath: String): Try[Zipper] = {
    addItem(item          = URLSource(url),
            path          = zipPath,
            flatten       = false,
            forceRoot     = Some("/"))
  }

  /** Add a `grizzled.net.URL` to the `Zipper`. This method is just
    * shorthand for:
    *
    * {{{
    * val gurl = grizzled.net.URL(...)
    * zipper.addURL(gurl.javaURL)
    * }}}
    *
    * @param url     the URL to the resource to be added
    * @param zipPath the path within the zip file for the entry
    *
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addURL(url: grizzled.net.URL, zipPath: String): Try[Zipper] = {
    addURL(url.javaURL, zipPath)
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
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addSource(source: Source, zipPath: String): Try[Zipper] = {
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
  def addSource(source:  Source,
                zipPath: String,
                flatten: Boolean): Try[Zipper] = {
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
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addInputStream(inputStream: InputStream, zipPath: String): Try[Zipper] = {
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
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addInputStream(inputStream: InputStream,
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
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addReader(reader: Reader, zipPath: String): Try[Zipper] = {
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
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addReader(reader: Reader, zipPath: String, flatten: Boolean): Try[Zipper] = {
    addItem(ReaderSource(reader), zipPath, flatten)
  }

  /** Add an array of bytes to the `Zipper`. The bytes constitute an eventual
    * entry in a zip file; a reference to the byte array is held within this
    * `Zipper` until it is garbage-collected.
    *
    * @param bytes    the array of bytes representing the entry to be written
    *                 to the zip file
    * @param zipPath  the path for the entry in the zip file
    */
  def addBytes(bytes: Array[Byte], zipPath: String): Try[Zipper] = {
    addItem(BytesSource(bytes), zipPath, flatten = false)
  }

  /** Recursively add all the files in a directory to the `Zipper`. Does not
    * currently work properly on Windows.
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
    *
    * @return A `Success` with the number of files found and added, or
    *         `Failure` on error.
    */
  def addDirectory(dir:      File,
                   strip:    Option[String] = None,
                   flatten:  Boolean = false,
                   wildcard: Option[String] = None): Try[Zipper] = {

    def addRecursively(dir: File, flatten: Boolean): Try[Zipper] = {

      @tailrec
      def addNext(stream: Stream[File], currentZipper: Zipper): Try[Zipper] = {

        def wildcardMatch(f: File): Boolean = {
          wildcard.forall(pat => fileutil.fnmatch(f.getName, pat))
        }

        stream match {
          case s if s.isEmpty               => Success(currentZipper)
          case s if s.head.isDirectory      => addNext(s.tail, currentZipper)
          case s if ! wildcardMatch(s.head) => addNext(s.tail, currentZipper)
          case s =>
            val f = s.head       // the next file or directory (File)
            val path = f.getPath // its path (String)
            val t = if (flatten)
              currentZipper.addFile(f, flatten = true)
            else if (strip.isDefined && strip.exists(p => path.startsWith(p)))
              currentZipper.addFile(f, path.substring(strip.get.length))
            else
              currentZipper.addFile(f)

            t match {
              case Failure(ex) => Failure(ex)
              case Success(z)  => addNext(s.tail, z)
            }
        }
      }

      addNext(fileutil.listRecursively(dir), this)
    }

    // Main logic

    for { _         <- dir.pathExists
          newZipper <- addRecursively(dir, flatten) }
    yield newZipper
  }

  /** Add a directory entry to the `Zipper`. The path should be in "/" form,
    * even on Windows, since zip and jar files always use "/". Any leading
    * "/" will be removed, converting it to a relative path.
    *
    * @param path the path of the directory entry to add
    * @return A `Success` with a new `Zipper` object, on success. A
    *         `Failure` on error. The original `Zipper` is not modified.
    */
  def addZipDirectory(path: String): Try[Zipper] = {
    def ensureNotThere(path: String): Try[Unit] = {
      if (bareDirectories contains path) {
        Failure(new Exception(s"""Zipper already contains directory "$path""""))
      }
      else {
        Success(())
      }
    }

    if (path.isEmpty) {
      Failure(new IOException("Cannot add empty directory entry."))
    }
    else if (path.last != '/') {
      Failure(new IOException(
        s"""Zip directory entry $path doesn't end in '/'."""
      ))
    }
    else {
      for { p <- stripRoot(path, forceRoot = Some("/"))
            _ <- ensureNotThere(p) }
      yield new Zipper(items = items, bareDirectories = bareDirectories + p)
    }
  }

  /** The unique paths in the `Zipper`. The directory entries will be
    * suffixed with "/". Note that intermediate directory entries will ''not''
    * be represented in this list. Only the paths that have been explicitly
    * added are represented.
    */
  val paths = {
    bareDirectories ++ items.keySet
  }

  /** Set the comment to be written to the zip or jar file.
    *
    * @param comment the comment.
    *
    * @return a new `Zipper` with the comment. This operation cannot fail,
    *         so the new value is returned without being wrapped in a `Try`.
    */
  def setComment(comment: String): Zipper = {
    new Zipper(items           = this.items,
               bareDirectories = this.bareDirectories,
               comment         = Some(comment))
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
    * @return  or
    */
  private def writeZipOutputStream(zo: ZipOutputStream): Try[Int] = {

    // Create the directories for a given path (like mkdir -p), skipping any
    // that have already been created. There's no need for this function to
    // be tail-recursive; there aren't likely to be enough directories to
    // blow the stack. Making it tail-recursive complicates (uglifies) the
    // logic.
    def makeDirs(path: String, existing: Set[String]): Try[Set[String]] = {

      def makeNext(subdirs: List[String], existing: Set[String]):
        Try[Set[String]] = {

        def mapDir(dir: String) = if (dir.last == '/') dir else dir + "/"

        subdirs match {
          case Nil => Success(existing)

          case dir :: rest if (dir == ".") || (dir == "..") =>
            makeNext(rest, existing)

          case dir :: rest if existing contains mapDir(dir) =>
            makeNext(rest, existing)

          case dir :: rest =>
            val dir2 = mapDir(dir)
            val entry = new ZipEntry(dir2)
            for { _   <- Try { zo.putNextEntry(entry) }
                  _   <- Try { zo.closeEntry() }
                  res <- makeNext(rest, existing + dir2) }
            yield res
        }
      }

      val components = new File(path).dirname.split
      val subdirs = for (i <- components.indices)
        yield components.slice(0, i + 1).mkString("/")

      makeNext(subdirs.toList, existing)
    }

    // Zip all the files, creating their directories as necessary.
    def zipItems(entries:      List[ZipSource],
                 countSoFar:   Int,
                 existingDirs: Set[String]): Try[(Int, Set[String])] = {
      def makeEntry(s: ZipSource): Try[Int] = {
        s.source.copyToZip(s.zipPath, zo)
      }

      entries match {
        case Nil => Success((countSoFar, existingDirs))
        case e :: rest =>
          for { newDirs <- makeDirs(e.zipPath, existingDirs)
                n       <- makeEntry(e)
                res     <- zipItems(rest, n + countSoFar, newDirs) }
          yield res

      }
    }

    // Create the explicitly-specified directory entries.

    def makeBareDirectories(dirs:         List[String],
                            countSoFar:   Int,
                            existingDirs: Set[String]): Try[Int] = {
      dirs match {
        case Nil => Success(countSoFar)
        case dir :: rest =>
          val d = if (dir endsWith "/") dir else dir + "/"
          if (existingDirs contains d)
            makeBareDirectories(rest, countSoFar, existingDirs)
          else {
            val entry = new ZipEntry(d)
            for { _   <- Try { zo.putNextEntry(entry) }
                  _   <- Try { zo.closeEntry() }
                  res <- makeBareDirectories(rest, countSoFar + 1,
                                             existingDirs + d) }
            yield res
          }
      }
    }

    def maybeAddComment(): Try[Unit] = {
      Try {
        this.comment.foreach(zo.setComment)
      }
    }

    for { _           <- maybeAddComment()
          sortedItems  = items.values.toList.sorted(ZipSourceOrdering)
          (n1, dirs)  <- zipItems(sortedItems, 0, Set.empty[String])
          n2          <- makeBareDirectories(bareDirectories.toList, 0, dirs) }
    yield n1 + n2
  }

  /** Add a wrapped item to the Zipper.
    *
    * @param item      the item to add, wrapped in an `ItemSource`
    * @param path      the path for the item in the zip file
    * @param flatten   whether or not to flatten the path
    * @param forceRoot if not None, bypass the file system root, using this one
    *                  instead. (Useful with URLs, which always use a root of
    *                  "/", regardless of the current operating system.)
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
          Success(p.replace(File.separatorChar, '/'))
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
    * @return A `Success` with the new path, or a `Failure` on error.
    */
  private def stripRoot(path: String, forceRoot: Option[String]): Try[String] = {
    val f = new File(path)

    def stripThisRoot(root: String): Try[String] = {
      if (path startsWith root)
        Success(path.substring(root.length))
      else
        Failure(new Exception(s""""$path" does not start with root "$root"."""))
    }

    val strippedPath = if (! f.isAbsolute) {
      // Already relative. Use as is.
      Success(path)
    }
    else {
      forceRoot.map(stripThisRoot).getOrElse {
        // Absolute and no root defined. Strip any leading file system roots.

        val lcPath = path.toLowerCase
        val matchingRoot = File.listRoots().filter { root =>
          val rootPath = root.getPath
          lcPath startsWith rootPath.toLowerCase
        }

        if (matchingRoot.isEmpty) {
          Failure(new IllegalArgumentException(
            s"""Absolute path "$path" does not match a file system root."""
          ))
        }
        else {
          Success(path.substring(matchingRoot.head.getPath.length))
        }
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
    * @param files    the list of `File` to add to the `Zipper`. The
    *                 existence of these files isn't verified until the zip
    *                 or jar file is written.
    * @param flatten  whether or not to flatten the paths.
    * @return A `Success` with the filled `Zipper`, or a `Failure` if one or
    *         more of the paths could not be added.
    */
  def apply(files: Array[File], flatten: Boolean): Try[Zipper] = {
    val toAdd = files.map { f =>
      if (flatten)
        (f, fileutil.basename(f.getPath))
      else
        (f, f.getPath)
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
          z.addFile(f, p.replace(File.pathSeparatorChar, '/')) match {
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
private[zip] trait InputStreamHelper {
  self: ItemSource =>

  /** Utility function to read an `InputStream`, copy it to a consumer, and
    * close it.
    *
    * @param is       the input stream
    * @param consumer the consumer
    *
    * @return A `Try` of the number of bytes read
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

    readNext(0).map { n =>
      is.close()
      n
    }
  }
}

/** An `ItemSource` that contains and knows how to read from a URL.
  *
  * @param url the JavaURL
  */
private[zip] final case class URLSource(url: JavaURL) extends ItemSource
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
private[zip] final case class FileSource(file: File) extends ItemSource
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
private[zip] final case class InputStreamSource(is: InputStream)
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
private[zip] final case class ReaderSource(r: Reader) extends ItemSource {
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

    readNext(0).map { n =>
      r.close()
      n
    }
  }
}

/** An `ItemSource` that contains and knows how to read from a
  * `scala.io.Source`
  *
  * @param source  the source
  */
private[zip] final case class SourceSource(source: Source) extends ItemSource {
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

    readNext(0).map { n =>
      source.close()
      n
    }
  }
}

/** An `ItemSource` that reads from a buffer of bytes.
  *
  * @param bytes  the byte array
  */
private[zip] final case class BytesSource(bytes: Array[Byte])
  extends ItemSource {

  def read(consumer: (Array[Byte], Int) => Try[Int]): Try[Int] =
    consumer(bytes, bytes.length)
}

/** The class for items that are added to a `Zipper`.
  *
  * @param source   the `ItemSource` container for the resource to be read
  * @param zipPath  the path in the zip file
  */
private[zip] final case class ZipSource(source: ItemSource, zipPath: String)

private[zip] object ZipSourceOrdering extends Ordering[ZipSource] {
  def compare(a: ZipSource, b: ZipSource) = a.zipPath compare b.zipPath
}
