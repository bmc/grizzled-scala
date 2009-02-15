package grizzled

import scala.util.matching.Regex

import java.io.File

object util
{
    /**
     * Indicator of current operating system.
     *
     * <ul>
     *   <li>VMS - OpenVMS
     *   <li>Windows - Microsoft Windows, other than Windows CE
     *   <li>WindowsCE - Microsoft Windows CE
     *   <li>OS2 - OS2
     *   <li>NetWare - NetWare
     *   <li>Mac - Mac OS, prior to Mac OS X
     *   <li>Posix - Anything Unix-like, including Mac OS X
     */
    object OperatingSystem extends Enumeration
    {
        val Posix = Value("Posix")
        val Mac = Value("Mac OS")
        val Windows = Value("Windows")
        val WindowsCE = Value("Windows CE")
        val OS2 = Value("OS/2")
        val NetWare = Value("NetWare")
        val VMS = Value("VMS")
    }

    import OperatingSystem._

    /**
     * Indicator of the current operating system, as defined by the
     * <tt>OperatingSystem</tt> enumeration.
     */
    val os = System.getProperty("os.name").toLowerCase match
    {
        case "mac" => Mac
        case "windows ce" => WindowsCE
        case "windows" => Windows
        case "os/2" => OS2
        case "netware" => NetWare
        case "openvms" => VMS
        case _ => Posix
    }

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
     * contain simple shell-style wildcards. See {@link #fnmatch}.
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
        println("path=" + path + ", pattern=" + pattern + ", matches=" + pattern.matcher(path).find)
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
     * <p>Similar to Python's <tt>fnmatch()</tt> function, this function
     * determines whether a name (strint) matches a wildcard pattern.</p>
     *
     * <p>Patterns are Unix shell-style:</p>
     *
     * <table border="0">
     *   <tr>
     *     <td align="right">*</td>
     *     <td align="left">matches everything</td>
     *   </tr>
     *   <tr>
     *     <td align="right">?</td>
     *     <td align="left">matches any single character</td>
     *   </tr>
     *   <tr>
     *     <td align="right">[set]</td>
     *     <td align="left">matches any character in <i>set</i></td>
     *   </tr>
     *   <tr>
     *     <td align="right">[!set]</td>
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

        // Convert to regular expression pattern.

        val regexPattern = "^" +
                           pattern.replace("\\", "\\\\")
                                   .replace(".", "\\.")
                                   .replace("*", ".*")
                                   .replace("[!", "[^")
                                   .replace("?", ".") +
                           "$";
        val flags = if (os == Posix) Pattern.CASE_INSENSITIVE else 0
        val re = Pattern.compile(regexPattern, flags)
        re.matcher(name).matches
    }
}
