/*---------------------------------------------------------------------------*\
  This software is released under a BSD-style license:

  Copyright (c) 2009 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2009 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "The Grizzled Scala Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
\*---------------------------------------------------------------------------*/

package grizzled.net

import grizzled.file.{util => FileUtil}

import java.net.URL
import java.io.{File, IOException, FileInputStream}

/**
 * URL-related utility methods.
 */
object url
{
    private lazy val ExtRegexp = """^(.*)(\.[^.]+)$""".r

    private[net] def getOutputFile(url: URL): File =
    {
        val urlPath = url.getPath
        val extension = urlPath match
        {
            case ExtRegexp(pathNoExt, ext) => ext
            case _                         => ".dat"
        }

        urlPath(urlPath.length - 1) match
        {
            case '/' => File.createTempFile("urldownload", extension)
            case _   => new File(FileUtil.joinPath(
                            System.getProperty("java.io.tmpdir"),
                            FileUtil.basename(urlPath)))
        }
    }

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
        val output = getOutputFile(new URL(url))

        download(new URL(url), output)
        output
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

        val dir = new File(FileUtil.dirname(pathOut.getCanonicalPath))
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
            FileUtil.copyStream(in, out)
        }

        finally
        {
            out.close()
            in.close()
        }
    }
}
