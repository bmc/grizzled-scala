package grizzled.net

import grizzled.file

import java.net.URL
import java.io.{File, IOException}

/**
 * URL-related utility methods.
 */
object url
{
    private lazy val ExtRegexp = """^(.*)(\.[^.]+)$""".r

    /**
     * Download the specified URL to a file. The name of the file is
     * taken from the URL, if possible, or generated otherwise.
     *
     * @param url  The string containing the URI/URL
     *
     * @return the file to which the output was written.
     */
    def download(url: String): File =
    {
        val u = new URL(url)
        val urlPath = u.getPath
        val extension = urlPath match
        {
            case ExtRegexp(pathNoExt, ext) => ext
            case _                         => ".dat"
        }

        val pathOut = urlPath(urlPath.length - 1) match
        {
            case '/' => File.createTempFile("urldownload", extension)
            case _   => new File(file.joinPath(
                            System.getProperty("java.io.tmpdir"),
                            file.basename(urlPath)))
        }

        download(new URL(url), pathOut)
        pathOut
    }

    /**
     * Download the specified URL to a file.
     *
     * @param url      The string containing the URI/URL
     * @param pathOut  Path of file to receive the output. If the file
     *                 exists, it is overwritten. If the file does not
     *                 exist, it is created. If any of the directories in
     *                 the path do not exist, they are created.
     */
    def download(url: String, pathOut: String): Unit =
        download(new URL(url), new File(pathOut))

    /**
     * Download the specified URL to a file.
     *
     * @param url      The URL
     * @param pathOut  Path of file to receive the output. If the file
     *                 exists, it is overwritten. If the file does not
     *                 exist, it is created. If any of the directories in
     *                 the path do not exist, they are created.
     */
    def download(url: URL, pathOut: String): Unit =
        download(url, new File(pathOut))

    /**
     * Download the specified URL to a file.
     *
     * @param url      The string containing the URI/URL
     * @param pathOut  File to receive the output. If the file exists, it
     *                 is overwritten. If the file does not exist, it is
     *                 created. If any of the directories in the path do not
     *                 exist, they are created.
     */
    def download(url: String, pathOut: File): Unit =
        download(new URL(url), pathOut)

    /**
     * Download the specified URL to a file.
     *
     * @param url      The URL
     * @param pathOut  File to receive the output. If the file exists, it
     *                 is overwritten. If the file does not exist, it is
     *                 created. If any of the directories in the path do not
     *                 exist, they are created.
     */
    def download(url: URL, pathOut: File): Unit =
    {
        import java.io.{BufferedInputStream, BufferedOutputStream}
        import java.io.{FileOutputStream}

        if (pathOut.isDirectory)
            throw new IOException("Output file \"" + pathOut + "\" exists " +
                                  "and is a directory.")

        val dir = new File(file.dirname(pathOut.getCanonicalPath))
        if ((! dir.exists) && (! dir.mkdirs()))
            throw new IOException("Can't create either target directory \"" +
                                  dir.toString + "\" or one of its parents.")

        // Open the URL and the output file. (Do the URL first. That way, if it
        // fails, we don't get a zero-length output file.
        val in = new BufferedInputStream(url.openStream())
        val out = new BufferedOutputStream(new FileOutputStream(pathOut))

        // Copy and close.
        try
        {
            file.copyStream(in, out)
        }

        finally
        {
            out.close()
            in.close()
        }
    }
}
