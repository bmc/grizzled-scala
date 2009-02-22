package grizzled

import scala.util.matching.Regex

import java.io.{File, IOException}

class FileDoesNotExistException(message: String) extends Exception

/**
 * Useful file-related utility functions.
 */
object file
{
    import string._ // Grizzled string functions

    /**
     * File separator. Exactly like Java's <tt>File.separator</tt>.
     * If system property "grizzled.file.separator" is set,
     * however, this value is set to "grizzled.file.separator", instead.
     * (This behavior is useful for testing.)
     */
    def fileSeparator: String =
    {
        val s = System.getProperty("grizzled.file.separator")
        if (s == null)
            File.separator
        else if (s.length == 0)
            File.separator
        else if (s.length == 1)
            s
        else
            throw new IllegalArgumentException("Bad file separator: " + s)
    }

    def fileSeparatorChar = fileSeparator(0)

    /**
     * Get the Java system properties as a Scala iterable. The iterable
     * will produce a (name, value) tuple.
     *
     * @return the system properties as an iterable
     */
    def systemProperties: Iterable[(String, String)] =
    {
        import scala.collection.mutable.ArrayBuffer

        val enum = System.getProperties.propertyNames
        val result = new ArrayBuffer[(String, String)]()

        while (enum.hasMoreElements)
        {
            val name = enum.nextElement.toString
            val value = System.getProperty(name)
            result += (name, value)
        }

        result.toList
    }

    /**
     * Get the directory name of a pathname.
     *
     * @param path path (absolute or relative)
     *
     * @return the directory portion
     */
    def dirname(path: String): String =
    {
        if ((path == null) || (path.length == 0))
            ""
        else if (! path.contains(fileSeparator))
            "."
        else
        {
            val components = splitPath(path)
            val len = components.length
            val result = components.take(len - 1) mkString fileSeparator
            if (result.length == 0)
                fileSeparator
            else
                result
        }
    }

    /**
     * Get the basename (file name only) part of a path.
     *
     * @param path  the path (absolute or relative)
     *
     * @return the file name portion
     */
    def basename(path: String): String =
    {
        if ((path == null) || (path.length == 0))
            ""
        else if (! (path contains fileSeparator))
            path
        else
        {
            val components = splitPath(path)
            components.drop(components.length - 1) mkString fileSeparator
        }
    }

    /**
     * Split a path into directory (dirname) and file (basename) components.
     * Analogous to Python's <tt>os.path.pathsplit()</tt> function.
     *
     * @param path  the path to split
     *
     * @return a (dirname, basename) tuple of strings
     */
    def dirnameBasename(path: String): (String, String) =
    {
        if ((path == null) || (path.length == 0))
            ("", "")
        else if ((path == ".") || (path == ".."))
            (path, "")
        else if (! (path contains fileSeparator))
            (".", path)
        else if (path == fileSeparator)
            (fileSeparator, "")
        else 
        {
            // Use a character to split, so it's not interpreted as a regular
            // expression (which causes problems with a Windows-style "\".
            // NOTE: We deliberately don't use splitPath() here.
            val components = (path split fileSeparator(0)).toList

            if (components.length == 1)
                (components(0), "")
            else
            {
                val listTuple = components splitAt (components.length - 1)
                val s: String = listTuple._1 mkString fileSeparator
                val prefix = 
                    if ((s.length == 0) && (path startsWith fileSeparator))
                        fileSeparator
                    else
                        s
                (prefix, listTuple._2 mkString fileSeparator)
            }
        }
    }

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
     * the <tt>glob()</tt> function, as well as a special "**" wildcard that
     * recursively matches any directory. (Think "ant".) Adapted from the
     * function of the same name in the Grizzled Python Utility Library.
     *
     * @param pattern   The wildcard pattern
     * @param directory The directory in which to do the globbing
     *
     * @return list of matches, or an empty list for none
     */
    def eglob(pattern: String, directory: String): List[String] =
    {
        def doGlob(pieces: List[String], directory: String): List[String] =
        {
            import scala.collection.mutable.ArrayBuffer

            val fDir = new File(directory)
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
                    if ((matches.length == 1) && (matches(0) == path))
                        result += piece // without the path info

                    else if (last)
                        result ++= matches

                    else
                    {
                        val remainingPieces = pieces.drop(1)
                        for (m <- matches;
                             if (new File(m).isDirectory))
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

        if (pattern.length == 0)
            List(".")

        else
        {
            // Split into pieces.
            val pieces = splitPath(pattern)
            doGlob(pieces.toList, directory)
        }
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
        import grizzled.sys
        import grizzled.sys.OperatingSystem._

        // Convert to regular expression pattern.

        val caseConv: String => String =
            if (sys.os == Posix)
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

        for (name <- names)
        {
            val f = new File(top + fileSeparator + name)
            if (f.isDirectory)
                dirs += name
            else
                nondirs += name
        }

        if (topdown)
            result += (top, dirs.toList, nondirs.toList)
            
        for (name <- dirs)
            result ++= walk(top + fileSeparator + name, topdown)
        
        if (! topdown)
            result += (top, dirs.toList, nondirs.toList)

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
     * @param path  the path
     *
     * @return the component pieces.
     */
    def splitPath(path: String): List[String] =
    {
        // Split with the path separator character, rather than the path
        // separator string. Using the string causes Scala to interpret it
        // as a regular expression, which causes problems when the separator
        // is a backslash (as on Windows). We could escape the backslash,
        // but it's just as easy to split on the character, not the string,
        
        import grizzled.sys.os
        import grizzled.sys.OperatingSystem._

        // Special case for Windows. (Stupid drive letters.)

        val (prefix, usePath) = 
            os match
            {
                case Windows => splitDrivePath(path)
                case _       => ("", path)
            }

        // If there are leading file separator characters, split() will
        // produce extra empty array elements. Prevent that.

        val subpath = usePath.foldLeft("") 
        {
            (c1, c2) => // Note: c1 and c2 are strings, not characters
            if (c1 == fileSeparator)
                c2.toString
            else
                c1 + c2
        }

        val absolute = path.startsWith(fileSeparator) || (prefix != "")
        val pieces = (subpath split fileSeparatorChar).toList
        if (absolute)
        {
            if (pieces.length == 0)
                List[String](prefix + fileSeparator)
            else
                (prefix + fileSeparator + pieces.head) :: pieces.tail
        }

        else
            pieces
    }

    /**
     * Join components of a path together.
     *
     * @param pieces  path pieces
     *
     * @return a composite path
     */
    def joinPath(pieces: String*) = pieces mkString fileSeparator

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
                                                targetDir + "\" does not exist.")

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
     * Copy a source file to a target file, using binary copying.
     *
     * @param sourcePath  path to the source file
     * @param targetPath  path to the target file
     */
    def copyFile(sourcePath: String, targetPath: String)
    {
        import java.io.{BufferedInputStream, BufferedOutputStream}
        import java.io.{FileInputStream, FileOutputStream}

        val in = new BufferedInputStream(new FileInputStream(sourcePath))
        val out = new BufferedOutputStream(new FileOutputStream(targetPath))

        try
        {
            var c: Int = in.read()
            while (c != -1)
            {
                out.write(c)
                c = in.read()
            }
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

        var files = fDir.list
        for (name <- files)
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
        if (path.length == 0)
            ("", "")
        else if (path(0) == ':')
            ("", path.drop(1))
        else if ((path.length > 1) && (path(1) == ':'))
            (path.take(2), path.drop(2))
        else
            ("", path)
    }

    /**
     * Normalize a path, eliminating double slashes, etc.
     *
     * @param path   the path
     *
     * @return the normalized path
     */
    def normalizePath(path: String): String =
    {
        import grizzled.sys.os
        import grizzled.sys.OperatingSystem._

        os match
        {
            case Posix => normalizePosixPath(path)
            case Windows => normalizeWindowsPath(path)
            case _ => throw new UnsupportedOperationException(
                "Unknown OS: " + os)
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

        var (prefix, newPath) = splitDrivePath(path)
        if (prefix.length == 0)
        {
            // No drive letter - preserve initial backslashes

            while ((newPath.length > 0) && (newPath(0) == '\\'))
            {
                prefix += newPath.take(1)
                newPath = newPath.drop(1)
            }
        }
        else
        {
            // We have a drive letter. Collapse initial backslashes

            if (newPath.startsWith("\\"))
            {
                prefix += "\\"
                while (newPath.startsWith("\\"))
                    newPath = newPath.drop(1)
            }
        }

        import scala.collection.mutable.ListBuffer

        val pieces = newPath.split("\\\\")
        val newPieces = new ListBuffer[String]()
        for (piece <- pieces
             if ((piece != "") && (piece != ".")))
        {
            if (piece == "..")
            {
                val last = newPieces.length - 1
                if ((newPieces.length > 0) && (newPieces(last) != ".."))
                    newPieces.remove(last)

                else if (! ((newPieces.length == 0) && (prefix.endsWith("\\"))))
                    newPieces += piece
            }

            else
                newPieces += piece
        }

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
                // three or more as a single slash.

                val initialSlashes =
                    if (path.startsWith("//") && (! path.startsWith("///")))
                        2
                    else if (path.startsWith("/"))
                        1
                    else
                        0
                var newPieces = new ListBuffer[String]()
                for (piece <- path.split("/");
                     if ((piece != ".") && (piece != "")))
                {
                    val newLength = newPieces.length
                    if ((piece != "..") ||
                        ((initialSlashes == 0) && (newLength == 0)) ||
                        ((newLength > 0) && (newPieces(newLength - 1) == "..")))
                        newPieces += piece
                    else if (newLength > 0)
                        newPieces.remove(0)
                }

                if (initialSlashes > 0)
                    ("/" * initialSlashes) + (newPieces mkString "/")
                else
                    newPieces mkString "/"
        }
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
    def universalPath(path: String): String =
    {
        import grizzled.sys.os
        import grizzled.sys.OperatingSystem._

        os match
        {
            case Posix => path
            case Windows => path.replace(fileSeparator, "/")
            case _ => throw new UnsupportedOperationException(
                "Unknown OS: " + os)
        }
    }

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
    def nativePath(path: String): String =
    {
        import grizzled.sys.os
        import grizzled.sys.OperatingSystem._

        os match
        {
            case Posix => path
            case Windows => path.replace("/", fileSeparator)
            case _ => throw new UnsupportedOperationException(
                "Unknown OS: " + os)
        }
    }
}
