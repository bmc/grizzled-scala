package grizzled.security

import java.io.{File, FileInputStream, InputStream}
import java.security.{MessageDigest => JMessageDigest}

import scala.annotation.tailrec
import scala.io.Source

import grizzled.string.{util => StringUtil}

/** A message digest producer.
  */
class Digester(dg: JMessageDigest) {

  /** Create a digest of a string, returning the digest bytes.
    *
    * @param string the input string
    *
    * @return the digest bytes
    */
  def digest(string: String): Array[Byte] = {
    dg.synchronized {
      dg.reset()
      dg.update(string.getBytes)
      dg.digest
    }
  }

  /** Create a digest of a string, returning the digest string.
    *
    * @param string the input string
    *
    * @return the digest bytes
    */
  def digestString(string: String): String = {
    StringUtil.bytesToHexString(digest(string))
  }

  /** Create a digest of a file, returning the digest bytes.
    *
    * @param file  the file
    *
    * @return the digest bytes
    */
  def digest(file: File): Array[Byte] = digest(new FileInputStream(file))

  /** Create a digest of a string, returning the digest string.
    *
    * @param string the file
    *
    * @return the digest bytes
    */
  def digestString(file: File): String = {
    StringUtil.bytesToHexString(digest(file))
  }

  /** Create a digest of a `Source`. Note that sources are implicitly
    * character-based, not byte-based.
    *
    * @param source the `Source`
    *
    * @return the digest bytes
    */
  def digest(source: Source): Array[Byte] = {
    dg.synchronized {
      dg.reset()

      for (c <- source)
        dg.update(c.toByte)

      dg.digest
    }
  }

  /** Create a digest of a `Source`, returning the digest string. Note that
    * sources are implicitly character-based, not byte-based.
    *
    * @param source the `Source`
    *
    * @return the digest string
    */
  def digestString(source: Source): String = {
    StringUtil.bytesToHexString(digest(source))
  }

  /** Create a digest of an `InputStream`, returning the digest bytes.
    *
    * @param stream  the `InputStream`
    *
    * @return the digest bytes
    */
  def digest(stream: InputStream): Array[Byte] = {
    dg.synchronized {

      @tailrec def readNext(): Unit = {
        val c = stream.read()
        if (c > 0) {
          dg.update(c.toByte)
          readNext()
        }
      }

      dg.reset()
      readNext()
      dg.digest
    }
  }

  /** Create a digest of an `InputStream`, returning the digest string.
    *
    * @param stream  the `InputStream`
    *
    * @return the digest string
    */
  def digestString(stream: InputStream): String = {
    StringUtil.bytesToHexString(digest(stream))
  }
}

/** Convenience methods for generating crypto-hash (i.e., message digest)
  * strings (e.g., MD5, SHA256, etc.) from byte values. Sample use:
  *
  * {{{
  * // Generate an MD5 hash-string of a string
  *
  * val hash = MessageDigest("md5").digestString("foo")
  *
  * // Generate an SHA256 hash-string of a file's contents.
  *
  * val hash = MessageDigest("sha256").digestString(new File("/tmp/foo"))
  * }}}
  */
object MessageDigest {

  /** Get a `Digester` for the specified algorithm.
    *
    * @param algorithm Any algorithm string accepted by
    *                  `java.security.MessageDigest.getInstance()`
    *
    * @return the `Digester` object.
    */
  def apply(algorithm: String): Digester = {
    new Digester(JMessageDigest.getInstance(algorithm))
  }
}