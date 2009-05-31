package grizzled

import scala.util.matching.Regex

import grizzled.sys.os
import grizzled.sys.OperatingSystem._

import java.io.{File, IOException}

class FileDoesNotExistException(message: String) extends Exception

/**
 * Useful file-related utility functions.
 */
object file
{
    import string._ // Grizzled string functions

    val fileSeparator = File.separator
    val fileSeparatorChar = fileSeparator(0)

    /**
     * Get the directory name of a pathname.
     *
     * @param path    path (absolute or relative)
     * @param fileSep the file separator to use
     *
     * @return the directory portion
     */
    def dirname(path: String, fileSep: String): String =
    {
        val components = splitPath(path, fileSep) 
        components match
        {
            case Nil => 
                ""

            case List("") =>
                ""

            case simple :: Nil if (! (simple startsWith fileSep)) =>
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

    /**
     * Get the directory name of a pathname, using the file separator of the
     * current running system.
     *
     * @param path path (absolute or relative)
     *
     * @return the directory portion
     */
    def dirname(path: String): String = dirname(path, fileSeparator)

    /**
     * Get the basename (file name only) part of a path.
     *
     * @param path    the path (absolute or relative)
     * @param fileSep the file separator to use
     *
     * @return the file name portion
     */
    def basename(path: String, fileSep: String): String =
    {
        val components = splitPath(path, fileSep)
        components match
        {
            case Nil =>
                ""
            case List("") =>
                ""
            case simple :: Nil if (! (simple startsWith fileSep)) =>
                path
            case _ =>
                components.drop(components.length - 1) mkString fileSep
        }
    }

    /**
     * Get the basename (file name only) part of a path, using the file
     * separator of the current running system.
     *
     * @param path  the path (absolute or relative)
     *
     * @return the file name portion
     */
    def basename(path: String): String = basename(path, fileSeparator)

    /**
     * Split a path into directory (dirname) and file (basename) components.
     * Analogous to Python's <tt>os.path.pathsplit()</tt> function.
     *
     * @param path    the path to split
     * @param fileSep the file separator to use
     *
     * @return a (dirname, basename) tuple of strings
     */
    def dirnameBasename(path: String, fileSep: String): (String, String) =
    {
        if ((path == null) || (path.length == 0))
            ("", "")
        else if ((path == ".") || (path == ".."))
            (path, "")
        else if (! (path contains fileSep))
            (".", path)
        else if (path == fileSep)
            (fileSep, "")
        else 
        {
            // Use a character to split, so it's not interpreted as a regular
            // expression (which causes problems with a Windows-style "\".
            // NOTE: We deliberately don't use splitPath() here.
            val components = (path split fileSep(0)).toList

            if (components.length == 1)
                (components(0), "")
            else
            {
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

    /**
     * Split a path into directory (dirname) and file (basename) components.
     * Analogous to Python's <tt>os.path.pathsplit()</tt> function. Uses
     * the file separator of the current running system.
     *
     * @param path  the path to split
     *
     * @return a (dirname, basename) tuple of strings
     */
    def dirnameBasename(path: String): (String, String) =
        dirnameBasename(path, fileSeparator)

    /**
     * Return the current working directory, as an absolute path.
     *
     * @return the current working directory
     */
    def pwd: String = new File(".").getCanonicalPath

    /**
     * Return a list of paths matching a pathname pattern. The pattern may
     * contain simple shell-style wildcards. See <tt>fnmatch()</tt>.
     *
     * @param path  The path to expand.
     *
     * @return a list of possibly expanded file names
     */
    def glob(path: String): List[String] =
    {
        // This method is essentially a direct translation of the Python
        // glob.glob() function.

        def glob1(dirname: String, pattern: String): List[String] =
        {
            val dir = if (dirname.length == 0) pwd else dirname
            val names = new File(dir).list.toList

            if (names == null)
                Nil

            else
            {
                val names2 =
                    if (path(0) != '.')
                        names.filter(_(0) != '.')
                    else
                        names

                for (name <- names; if (fnmatch(name, pattern)))
                    yield name
            }
        }

        def glob0(dirname: String, basename: String): List[String] =
        {
            if (basename.length == 0)
            {
                if (new File(dirname).isDirectory)
                    List[String](basename)
                else
                    Nil
            }
            else
            {
                val path = dirname + fileSeparator + basename
                if (new File(path).exists())
                    List[String](basename)
                else
                    Nil
            }
        }

        val wildcards = """[\*\?\[]""".r
        if ((wildcards findFirstIn path) == None)
            List[String](path)

        else
        {
            val (dirname, basename) = dirnameBasename(path)
            if (dirname.length == 0)
                for (name <- glob1(pwd, basename)) yield name

            else
            {
                val dirs =
                    if ((wildcards findFirstIn dirname) != None)
                        glob(dirname)
                    else
                        List[String](dirname)
                val globber =
                    if ((wildcards findFirstIn basename) != None)
                        glob1 _
                    else
                        glob0 _
                 for (d <- dirs;
                      name <- globber(d, basename))
                     yield d + fileSeparator + name
             }
        }
    }

    /**
     * An extended <i>glob</i> function that supports all the wildcards of
     * the <tt>glob()</tt> function, in addition to:
     *
     * <ul>
     *  <li> a leading "~", signifying the user's home directory
     *  <li> a special "**" wildcard that recursively matches any directory.
     *       (Think "ant".)
     * </ul>
     *
     * "~user" is not supported, however.
     *
     * @param pattern   the wildcard pattern
     *
     * @return list of matches, or an empty list for none
     */
    def eglob(pattern: String): List[String] =
    {
        def doGlob(pieces: List[String], directory: String): List[String] =
        {
            import scala.collection.mutable.ArrayBuffer

            val result = new ArrayBuffer[String]()

            val piece = pieces(0)
            val last = (pieces.length == 1)

            if (piece == "**")
            {
                val remainingPieces = if (last) Nil else pieces.drop(1)

                for ((root, dirs, files) <- walk(directory, true))
                {
                    if (last)
                        // At the end of a pattern, "**" just recursively
                        // matches directories.
                        result += root

                    else
                        // Recurse downward, trying to match the rest of
                        // the pattern.
                        result ++= doGlob(remainingPieces, root)
                }
            }

            else
            {
                // Regular glob pattern.

                val path = directory + fileSeparator + piece
                val matches = glob(path)
                if (matches.length > 0)
                {
                    if (last)
                        // Save the matches, and stop.
                        result ++= matches

                    else
                    {
                        // Must continue recursing.
                        val remainingPieces = pieces.drop(1)
                        for (m <- matches; if (new File(m).isDirectory))
                        {
                            val subResult = doGlob(remainingPieces, m)
                            for (partialPath <- subResult)
                                result += partialPath
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
                normalizePath(joinPath(System.getProperty("user.home"),
                                       pattern drop 1))
            else
                pattern

        // Determine leading directory, which is different per OS (because
        // of Windows' stupid drive letters).
        val (relativePattern, directory) = eglobPatternSplitter(adjustedPattern)

        // Do the actual globbing.
        val pieces = splitPath(relativePattern)
        val matches = doGlob(pieces.toList, directory)

        matches map (normalizePath _)
    }

    /**
     * For the eglob algorithm to work, the pattern needs to be split into a
     * (directory, subpattern) pair, where the subpattern is relative. This
     * splitting operating is operating system-dependent, largely because
     * of Windows' stupid drive letters. This variable holds a partially
     * applied function for the splitter, determined the first time it is
     * referenced. That way, eglob() doesn't do this same match on every
     * call.
     */
    private lazy val eglobPatternSplitter = os match
    {
        case Posix   => splitPosixEglobPattern(_)
        case Windows => splitWindowsEglobPattern(_)
        case _       => throw new UnsupportedOperationException(
                            "Unknown OS: " + os)
    }

    /**
     * Windows pattern splitter for eglob(). See description for the
     * eglobPatternSplitter value, above.
     *
     * @param pattern  the pattern to split
     *
     * @return a (directory, subpattern) tuple
     */
    private def splitWindowsEglobPattern(pattern: String): (String, String) =
    {
        splitDrivePath(pattern) match
        {
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

    /**
     * Posix pattern splitter for eglob(). See description for the
     * eglobPatternSplitter value, above.
     *
     * @param pattern  the pattern to split
     *
     * @return a (directory, subpattern) tuple
     */
    private def splitPosixEglobPattern(pattern: String): (String, String) =
    {
        if (pattern.length == 0)
            (".", ".")

        else if (pattern(0) == fileSeparatorChar)
            (pattern drop 1, "/")

        else
            (pattern, ".")
    }

    /**
     * Similar to Python's <tt>fnmatch()</tt> function, this function
     * determines whether a string matches a wildcard pattern. Patterns
     * are Unix shell-style wildcards:
     *
     * <table border="0" cellspacing="2" class="list">
     *   <tr>
     *     <td align="right" class="code">*</td>
     *     <td align="left">matches everything</td>
     *   </tr>
     *   <tr>
     *     <td align="right" class="code">?</td>
     *     <td align="left">matches any single character</td>
     *   </tr>
     *   <tr>
     *     <td align="right" class="code">[set]</td>
     *     <td align="left">matches any character in <i>set</i></td>
     *   </tr>
     *   <tr>
     *     <td align="right" class="code">[!set]</td>
     *     <td align="left">matches any character not in <i>set</i></td>
     *   </tr>
     * </table>
     *
     * An initial period in <tt>filename</tt> not special. Matches are
     * case-sensitive on Posix operating systems, case-insensitive elsewhere.
     *
     * @param name    the name to match
     * @param pattern the wildcard pattern
     */
    def fnmatch(name: String, pattern: String): Boolean =
    {
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
        (regex findFirstIn caseConv(name)) != None
    }

    /**
     * Directory tree generator, adapted from Python's <tt>os.walk()</tt>
     * function.
     *
     * <p>For each directory in the directory tree rooted at top (including top
     * itself, but excluding '.' and '..'), yields a 3-tuple</p>
     *
     * <blockquote><pre>dirpath, dirnames, filenames</pre></blockquote>
     *
     * <p><i>dirpath</i> is a string, the path to the directory.
     * <i>dirnames</i> is a list of the names of the subdirectories in
     * <i>dirpath</i> (excluding '.' and '..'). <i>filenames</i> is a list
     * of the names of the non-directory files in <i>dirpath</i>. Note that
     * the names in the lists are just names, with no path components. To
     * get a full path (which begins with top) to a file or directory in
     * <i>dirpath</i>, <tt>dirpath + java.io.fileSeparator + name</tt>.</p>
     *
     * <p>If <i>topdown</i> is <tt>true</tt>, the triple for a directory is
     * generated before the triples for any of its subdirectories
     * (directories are generated top down). If <tt>topdown</tt> is
     * <tt>false</tt>, the triple for a directory is generated after the
     * triples for all of its subdirectories (directories are generated
     * bottom up).</p>
     *
     * <p><b>WARNING!</b> This method does <i>not</i> grok symlinks!
     *
     * @param top     name of starting directory
     * @param topdown <tt>true</tt> to do a top-down traversal, <tt>false</tt>
     *                otherwise
     *
     * @return iterator of triplets, as described above.
     */
    def walk(top: String, topdown: Boolean):
        List[(String, List[String], List[String])] =
    {
        // This needs to be made more efficient, with some kind of generator.
        import scala.collection.mutable.ArrayBuffer

        val dirs = new ArrayBuffer[String]()
        val nondirs = new ArrayBuffer[String]()
        val result = new ArrayBuffer[(String, List[String], List[String])]()
        val fTop = new File(top)
        val names = fTop.list

        if (names != null)
        {
            for (name <- names)
            {
                val f = new File(top + fileSeparator + name)
                if (f.isDirectory)
                    dirs += name
                else
                    nondirs += name
            }

            if (topdown)
                result += Tuple(top, dirs.toList, nondirs.toList)
            
            for (name <- dirs)
                result ++= walk(top + fileSeparator + name, topdown)
        
            if (! topdown)
                result += Tuple(top, dirs.toList, nondirs.toList)
        }

        result.toList
    }

    /**
     * Split a path into its constituent components. If the path is
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
     *      <td class="code">List[String]("")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"/"</td>
     *     <td class="code">List[String]("/")
     *   </tr>
     *   <tr>
     *     <td class="code">"foo"</td>
     *     <td class="code">List[String]("foo")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"foo/bar"</td>
     *     <td class="code">List[String]("foo", "bar")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"."</td>
     *     <td class="code">List[String](".")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"../foo"</td>
     *     <td class="code">List[String]("..", "foo")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"./foo"</td>
     *     <td class="code">List[String](".", "foo")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"/foo/bar/baz"</td>
     *     <td class="code">List[String]("/foo", "bar", "baz")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"foo/bar/baz"</td>
     *     <td class="code">List[String]("foo", "bar", "baz")</td>
     *   </tr>
     *   <tr>
     *     <td class="code">"/foo"</td>
     *     <td class="code">List[String]("/foo")</td>
     *   </tr>
     * </table>
     *
     * @param path    the path
     * @param fileSep the file separator to use
     *
     * @return the component pieces.
     */
    def splitPath(path: String, fileSep: String): List[String] =
    {
        // Split with the path separator character, rather than the path
        // separator string. Using the string causes Scala to interpret it
        // as a regular expression, which causes problems when the separator
        // is a backslash (as on Windows). We could escape the backslash,
        // but it's just as easy to split on the character, not the string,
        
        // Null guard.

        val nonNullPath = if (path == null) "" else path

        // Special case for Windows. (Stupid drive letters.)

        val (prefix, usePath) = 
            if (fileSep == "\\")
                splitDrivePath(nonNullPath)
            else
                ("", nonNullPath)

        // If there are leading file separator characters, split() will
        // produce extra empty array elements. Prevent that.

        val subpath = usePath.foldLeft("") 
        {
            (c1, c2) => // Note: c1 and c2 are strings, not characters

            if (c1 == fileSep)
                c2.toString
            else
                c1 + c2
        }

        val absolute = path.startsWith(fileSep) || (prefix != "")

        // Split on the character, not the full string. Splitting on a string
        // uses regular expression semantics, which will fail if this is
        // Windows (and the file separator is "\"). Windows is a pain in the
        // ass.
        val pieces = (subpath split fileSep(0)).toList
        if (absolute)
        {
            if (pieces.length == 0)
                List[String](prefix + fileSep)
            else
                (prefix + fileSep + pieces.head) :: pieces.tail
        }

        else
            pieces
    }

    /**
     * Split a path into its constituent components, using the file separator
     * of the currently running system. See the other version of
     * <tt>splitPath()</tt> for complete documentation.
     *
     * @param path    the path
     *
     * @return the component pieces.
     */
    def splitPath(path: String): List[String] = splitPath(path, fileSeparator)

    /**
     * Join components of a path together.
     *
     * @param fileSep the file separator to use
     * @param pieces  path pieces
     *
     * @return a composite path
     */
    def joinPath(fileSep: String, pieces: List[String]): String =
        pieces mkString fileSep

    /**
     * Join components of a path together, using the file separator of the
     * currently running system
     *
     * @param pieces  path pieces
     *
     * @return a composite path
     */
    def joinPath(pieces: String*): String =
        joinPath(fileSeparator, pieces.toList)

    /**
     * Copy multiple files to a target directory. Also see the version of this
     * method that takes only one file.
     *
     * @param files        An <tt>Iterable</tt> of file names to be copied
     * @param targetDir    Path name to target directory
     * @param createTarget <tt>true</tt> to create the target directory,
     *                     <tt>false</tt> to throw an exception if the
     *                     directory doesn't already exist.
     *
     * @throws FileDoesNotExistException a source file or the target directory
     *                                   doesn't exist
     * @throws IOException cannot create target directory
     */
    def copy(files: Iterable[String],
             targetDir: String,
             createTarget: Boolean): Unit =
    {
        val target = new File(targetDir)

        if ((! target.exists()) && (createTarget))
            if (! target.mkdirs())
                throw new IOException("Unable to create target directory \"" +
                                      targetDir + "\"")

        if (target.exists() && (! target.isDirectory()))
            throw new IOException("Cannot copy files to non-directory \"" +
                                  targetDir + "\"")

        if (! target.exists())
            throw new FileDoesNotExistException("Target directory \"" +
                                                targetDir + 
                                                "\" does not exist.")

        for (file <- files)
            copyFile(file, targetDir + fileSeparator + basename(file))
    }

    /**
     * Copy multiple files to a target directory. Also see the version of this
     * method that takes only one file. If the target directory does not exist,
     * it is created.
     *
     * @param files        An <tt>Iterable</tt> of file names to be copied
     * @param targetDir    Path name to target directory
     *
     * @throws FileDoesNotExistException a source file or the target directory
     *                                   doesn't exist
     * @throws IOException cannot create target directory
     */
    def copy(files: Iterable[String], targetDir: String): Unit =
        copy(files, targetDir, true)

    /**
     * Copy a file to a directory.
     *
     * @param file         Path name of the file to copy
     * @param targetDir    Path name to target directory
     * @param createTarget <tt>true</tt> to create the target directory,
     *                     <tt>false</tt> to throw an exception if the
     *                     directory doesn't already exist.
     *
     * @throws FileDoesNotExistException source file or target directory
     *                                   doesn't exist
     * @throws IOException cannot create target directory
     */
    def copy(file: String, targetDir: String, createTarget: Boolean): Unit =
        copy(List[String](file), targetDir, createTarget)

    /**
     * Copy a file to a directory. If the target directory does not exist,
     * it is created.
     *
     * @param file         Path name of the file to copy
     * @param targetDir    Path name to target directory
     *
     * @throws FileDoesNotExistException a source file or the target directory
     *                                   doesn't exist
     * @throws IOException cannot create target directory
     */
    def copy(file: String, targetDir: String): Unit =
        copy(file, targetDir, true)

    /**
     * Copy a source file to a target file, using binary copying. The source
     * file must be a file. The target path can be a file or a directory; if
     * it is a directory, the target file will have the same base name as
     * as the source file.
     *
     * @param sourcePath  path to the source file
     * @param targetPath  path to the target file or directory
     *
     * @return the full path of the target file
     */
    def copyFile(sourcePath: String, targetPath: String): String =
    {
        import java.io.{InputStream, OutputStream,
                        BufferedInputStream, BufferedOutputStream,
                        FileInputStream, FileOutputStream}

        val target = 
            if (new File(targetPath).isDirectory())
                joinPath(targetPath, basename(sourcePath))
            else
                targetPath

        val in = new BufferedInputStream(new FileInputStream(sourcePath))
        val out = new BufferedOutputStream(new FileOutputStream(target))

        try
        {
            def copyNextByte(in: InputStream, out: OutputStream): Unit =
            {
                val c: Int = in.read()
                if (c != -1)
                {
                    out.write(c)
                    copyNextByte(in, out)
                }
            }

            // Tail recursion means never having to use a var.
            copyNextByte(in, out)

            // Result
            target
        }

        finally
        {
            in.close()
            out.close()
        }
    }

    /**
     * Recursively copy a source directory and its contents to a target
     * directory. Creates the target directory if it does not exist.
     *
     * @param sourceDir  the source directory
     * @param targetDir  the target directory
     */
    def copyTree(sourceDir: String, targetDir: String)
    {
        val fSource = new File(sourceDir)

        if (! fSource.exists())
            throw new FileDoesNotExistException(sourceDir)

        if (! fSource.isDirectory)
            throw new IOException("Source directory \"" + sourceDir +
                                  "\" is not a directory.")

        val files = fSource.list
        new File(targetDir).mkdirs
        for (f <- files)
        {
            val sourceFilename = sourceDir + fileSeparator + f
            val targetFilename = targetDir + fileSeparator + f

            if (new File(sourceFilename).isDirectory)
                copyTree(sourceFilename, targetFilename)
            else
                copyFile(sourceFilename, targetFilename)
        }
    }

    /**
     * Recursively remove a directory tree. This function is conceptually
     * equivalent to <tt>rm -r</tt> on a Unix system.
     *
     * @param dir  The directory
     */
    def deleteTree(dir: String)
    {
        val fDir = new File(dir)
        if (! new File(dir).isDirectory)
            throw new IOException("\"" + dir + "\" is not a directory.")

        for (name <- fDir.list)
        {
            val fullPath = dir + fileSeparator + name
            val f = new File(fullPath)
            if (f.isDirectory)
                deleteTree(fullPath)
            else
                f.delete()
        }

        fDir.delete()
    }

    /**
     * Similar to the Unix <i>touch</i> command, this function:
     *
     * <ul>
     *   <li>updates the access and modification times for any existing files
     *       in a list of files
     *   <li>creates any non-existent files in the list of files
     * </ul>
     *
     * If any file in the list is a directory, this method will throw an
     * exception.
     *
     * @param files  Iterable of files to touch
     * @param time   Set the last-modified time to this time, or to the current
     *               time if this parameter is negative.
     *
     * @throws IOException on error
     */
    def touch(files: Iterable[String], time: Long): Unit =
    {
        val useTime = if (time < 0) System.currentTimeMillis else time
        for (name <- files)
        {
            val file = new File(name)

            if (file.isDirectory)
                throw new IOException("File \"" + name + "\" is a directory")

            if (! file.exists())
                file.createNewFile()

            file.setLastModified(useTime)
        }
    }

    /**
     * Similar to the Unix <i>touch</i> command, this function:
     *
     * <ul>
     *   <li>updates the access and modification times for any existing files
     *       in a list of files
     *   <li>creates any non-existent files in the list of files
     * </ul>
     *
     * If any file in the list is a directory, this method will throw an
     * exception.
     *
     * This version of <tt>touch()</tt> always set the last-modified time to
     * the current time.
     *
     * @param files  Iterable of files to touch
     *
     * @throws IOException on error
     */
    def touch(files: Iterable[String]): Unit = touch(files, -1)

    /**
     * Similar to the Unix <i>touch</i> command, this function:
     *
     * <ul>
     *   <li>updates the access and modification times for a file
     *   <li>creates the file if it does not exist
     * </ul>
     *
     * If the file is a directory, this method will throw an exception.
     *
     * @param path  The file to touch
     * @param time  Set the last-modified time to this time, or to the current
     *              time if this parameter is negative.
     *
     * @throws IOException on error
     */
    def touch(path: String, time: Long): Unit = touch(List[String](path), time)

    /**
     * Similar to the Unix <i>touch</i> command, this function:
     *
     * <ul>
     *   <li>updates the access and modification times for a file
     *   <li>creates the file if it does not exist
     * </ul>
     *
     * If the file is a directory, this method will throw an exception.
     *
     * This version of <tt>touch()</tt> always set the last-modified time to
     * the current time.
     *
     * @param path  The file to touch
     *
     * @throws IOException on error
     */
    def touch(path: String): Unit = touch(path, -1)

    private lazy val DrivePathPattern = "^([A-Za-z]?:)?(.*)$".r
    /**
     * Split a Windows-style path into drive name and path portions.
     *
     * @param path  the path
     *
     * @return a (drive, path) tuple, either component of which can be
     * *       an empty string
     */
    def splitDrivePath(path: String): (String, String) =
    {
        path match
        {
            case DrivePathPattern(driveSpec, path) =>
                driveSpec match
                {
                    case null => ("", path)
                    case ":"  => ("", path)
                    case _    => (driveSpec, path)
                }

            case _ => ("", path)
        }
    }

    /**
     * Path normalization is operating system-specific. This value
     * holds the real path normalizer, determined once.
     */
    private lazy val doPathNormalizing = os match
    {
        case Posix   => normalizePosixPath(_)
        case Windows => normalizeWindowsPath(_)
        case _       => throw new UnsupportedOperationException(
                            "Unknown OS: " + os)
    }

    /**
     * Normalize a path, eliminating double slashes, resolving embedded
     * ".." strings (e.g., "/foo/../bar" becomes "/bar"), etc. Works for
     * Windows and Posix operating systems.
     *
     * @param path  the path
     *
     * @return the normalized path
     */
    def normalizePath(path: String): String = doPathNormalizing(path)

    /**
     * Shared between normalizeWindowsPath() and normalizePosixPath(),
     * this function normalizes the pieces of a path, handling embedded "..",
     * empty elements (from splitting when there are adjacent file separators),
     * etc.
     *
     * @param pieces  path components, with no separators
     *
     * @return sanitized list of path components
     */
    private def normalizePathPieces(pieces: List[String]): List[String] =
    {
        pieces match
        {
            case Nil =>
                Nil

            case "" :: tail =>
                normalizePathPieces(tail)

            case "." :: tail =>
                normalizePathPieces(tail)

            case a :: ".." :: tail =>
                normalizePathPieces(tail)

            case _ =>
                List[String](pieces.head) ++ normalizePathPieces(pieces.tail)
        }
    }

    /**
     * Normalize a Windows path name. Handles UNC paths. Adapted from the
     * Python version of normpath() in Python's <tt>os.ntpath</tt> module.
     *
     * @param path   the path
     *
     * @return the normalized path
     */
    def normalizeWindowsPath(path: String): String =
    {
        // We need to be careful here. If the prefix is empty, and the path
        // starts with a backslash, it could either be an absolute path on
        // the current drive (\dir1\dir2\file) or a UNC filename
        // (\\server\mount\dir1\file). It is therefore imperative NOT to
        // collapse multiple backslashes blindly in that case. The code
        // below preserves multiple backslashes when there is no drive
        // letter. This means that the invalid filename \\\a\b is preserved
        // unchanged, where a\\\b is normalized to a\b.

        val (prefix, newPath) = splitDrivePath(path) match
        {
            case ("", path) =>
                // No drive letter - preserve initial backslashes

                (path takeWhile (_ == '\\') mkString "", 
                 path dropWhile (_ == '\\') mkString "")

            case (prefix, path) =>
                // We have a drive letter.

                (prefix + "\\", path dropWhile (_ == '\\') mkString "")
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
        if ((prefix.length == 0) && (newPieces.length == 0))
            "."
        else
            prefix + (newPieces mkString "\\")
    }

    /**
     * Adapted from the Python version of normpath() in Python's
     * <tt>os.posixpath</tt> module.
     *
     * @param path   the path
     *
     * @return the normalized path
     */
    def normalizePosixPath(path: String): String =
    {
        import scala.collection.mutable.ListBuffer

        path match
        {
            case "" => "."
            case "." => "."
            case _ =>
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
                    path.split("/").toList match
                    {
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

    /**
     * Native-to-universal path conversion is operating system-specific.
     * These values hold the real converters, determined once.
     */
    private lazy val makeUniversalPath: (String) => String = os match
    {
        case Posix   => (path: String) => path
        case Windows => (path: String) => path.replace(fileSeparator, "/")
        case _       => throw new UnsupportedOperationException(
                            "Unknown OS: " + os)
    }

    private lazy val makeNativePath: (String) => String = os match
    {
        case Posix   => (path: String) => path
        case Windows => (path: String) => path.replace("/", fileSeparator)
        case _       => throw new UnsupportedOperationException(
                            "Unknown OS: " + os)
    }

    /**
     * Converts a path name from its operating system-specific format to a
     * universal path notation. Universal path notation always uses a
     * Unix-style "/" to separate path elements. A universal path can be
     * converted to a native (operating system-specific) path via the
     * <tt>native_path()</tt> function. Note that on POSIX-compliant systems,
     * this function simply returns the <tt>path</tt> parameter unmodified.
     *
     * @param path the path to convert to universal path notation
     *
     * @return the universal path
     */
    def universalPath(path: String): String = makeUniversalPath(path)

    /**
     * Converts a path name from universal path notation to the operating
     * system-specific format. Universal path notation always uses a
     * Unix-style "/" to separate path elements. A native path can be
     * converted to a universal path via the <tt>universal_path()</tt>
     * function. Note that on POSIX-compliant systems, this function simply
     * returns the <tt>path</tt> parameter unmodified.
     *
     * @param path the path to convert from universtal to native path notation
     *
     * @return the native path
     */
    def nativePath(path: String): String = makeNativePath(path)
}
