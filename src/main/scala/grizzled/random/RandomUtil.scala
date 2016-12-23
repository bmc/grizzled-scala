package grizzled.random

import scala.util.Random

/** Utility functions for working with random numbers. These functions use
  * the default `scala.util.Random` object. To specify your own `Random`
  * instance, create an instance of the `RandomUtil` companion class.
  */
object RandomUtil extends RandomUtilFunctions {
  val rng = Random
}

/** Utility functions for working with random numbers. It's more convenient
  * to use the companion `RandomUtil` object, unless you need to specify your
  * own `scala.util.Random` instance.
  *
  * @param rng  the `scala.util.Random` instance to use. Defaults to the
  *             `scala.util.Random` object.
  */
class RandomUtil(val rng: Random = Random) extends RandomUtilFunctions

/** The trait that implements the actual random utility methods.
  */
trait RandomUtilFunctions {
  val rng: Random

  val DefaultRandomStringChars = "abcdefghijklmnopqrstuvwxyz" +
                                 "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                 "0123456789"

  /** Choose a random value from an array of values.
    *
    * @param a    the array
    * @tparam T   the type of the elements in the sequent
    *
    * @return a random value from the array
    */
  def randomChoice[T](a: Array[T]): T = a(rng.nextInt(a.length))

  /** Choose a random value from an indexed sequence of values.
    *
    * @param seq  the sequence
    * @tparam T   the type of the elements in the sequent
    *
    * @return a random value from the sequence
    */
  def randomChoice[T](seq: IndexedSeq[T]): T = seq(rng.nextInt(seq.length))

  /** Return a random integer between `low` and `high`, inclusive. If `low`
    * and `high` are identical, `low` is returned.
    *
    * @param low  the lowest number
    * @param high the highest number
    *
    * @return an integer in the range `[low, high]`.
    *
    * @throws IllegalArgumentException if `low` is greater than `high`.
    */
  def randomIntBetween(low: Int, high: Int): Int = {
    require(low <= high)
    if (low == high) low else rng.nextInt(high - low) + low
  }

  /** Return a random string. This method is similar to `Random.nextString()`,
    * except that it allows you to control the set of characters that are
    * allowed to be in the returned string. The set of characters defaults to
    * ASCII alphanumerics.
    *
    * @param length the size of the string to return
    * @param chars  the set of legal characters
    *
    * @return a random string, drawn from the supplied characters, of the
    *         specified length
    */
  def randomString(length: Int,
                   chars:  String = DefaultRandomStringChars): String = {
    require(chars.length > 0)
    if (chars.length == 1)
      chars.take(1) * length
    else
      (1 to length).map { _ => randomChoice(chars) }.mkString
  }
}
