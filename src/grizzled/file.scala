package grizzled

import scala.util.matching.Regex

import java.io.{File, IOException}

class FileDoesNotExistException(message: String) extends Exception

object file
{
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
        val sep = File.separator

        if ((path == null) || (path.length == 0))
            ""
        else if (! path.contains(sep))
            "."
        else
        {
            val components = path.split(sep)
            components.take(components.length - 1) mkString ("", sep, "")
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
        val sep = File.separator

        if ((path == null) || (path.length == 0))
            ""
        else if (! path.contains(sep))
            path
        else
        {
            val components = path.split(sep)
            components.drop(components.length - 1) mkString ("", sep, "")
        }
    }

    /**
     * Split a path into directory (dirname) and file (basename) components.
     *
     * @param path  the path to split
     *
     * @return a (dirname, basename) tuple of strings
     */
    def pathsplit(path: String): (String, String) =
    {
        val sep = File.separator

        if ((path == null) || (path.length == 0))
            ("", "")
        else if (! path.contains(sep))
            (".", path)
        else
        {
            val components = path.split(sep).toList
            val listTuple = components.splitAt(components.length - 1)

            (listTuple._1 mkString ("", sep, ""),
             listTuple._2 mkString ("", sep, ""))
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

        val sep = File.separator

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
                val f = new File(dirname)
                if (f.isDirectory)
                    List[String](basename)
                else
                    Nil
            }
            else
            {
                val path = dirname + sep + basename
                if (new File(path).exists())
                    List[String](basename)
                else
                    Nil
            }

        }

        val wildcards = new Regex("[\\*\\?\\[]")
        val pattern = wildcards.pattern
        if (! pattern.matcher(path).find)
            List[String](path)

        else
        {
            val (dirname, basename) = pathsplit(path)
            if (dirname.length == 0)
                for (name <- glob1(pwd, basename)) yield name

            else
            {
                val dirs =
                    if (pattern.matcher(dirname).find)
                        glob(dirname)
                    else
                        List[String](dirname)
                val globber =
                    if (pattern.matcher(basename).find)
                        glob1 _
                    else
                        glob0 _
                 for (d <- dirs;
                      name <- globber(d, basename))
                     yield d + sep + name
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

                for (tuple <- walk(directory, true))
                {
                    val (root, dirs, files) = tuple

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

                val matches = glob(directory + File.separator + piece)
                if (matches.length > 0)
                {
                    if (last)
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

        // Split into pieces. Note: If there's a leading "/", split() will
        // produce an extra empty array element. Prevent that.
        val pieces =
            if (pattern(0) == File.separatorChar)
                pattern.slice(1, pattern.length).split(File.separator)
            else
                pattern.split(File.separator)

        doGlob(pieces.toList, directory)
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
        import java.util.regex.Pattern
        import grizzled.sys
        import grizzled.sys.OperatingSystem._

        // Convert to regular expression pattern.

        val regexPattern = "^" +
                           pattern.replace("\\", "\\\\")
                                   .replace(".", "\\.")
                                   .replace("*", ".*")
                                   .replace("[!", "[^")
                                   .replace("?", ".") +
                           "$";
        val flags = if (sys.os != Posix) Pattern.CASE_INSENSITIVE else 0
        val re = Pattern.compile(regexPattern, flags)
        re.matcher(name).matches
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
     * <i>dirpath</i>, <tt>dirpath + java.io.File.separator + name</tt>.</p>
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
            val f = new File(top + File.separator + name)
            if (f.isDirectory)
                dirs += name
            else
                nondirs += name
        }

        if (topdown)
            result += (top, dirs.toList, nondirs.toList)
            
        for (name <- dirs)
            result ++= walk(top + File.separator + name, topdown)
        
        if (! topdown)
            result += (top, dirs.toList, nondirs.toList)

        result.toList
    }

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
            copyFile(file, targetDir + File.separator + basename(file))
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
            val sourceFilename = sourceDir + File.separator + f
            val targetFilename = targetDir + File.separator + f

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
            val fullPath = dir + File.separator + name
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
}
