package grizzled.parsing

/** A simple string token class, consisting of:
  *
  * - a string token
  * - the starting position of the token in the original string from which
  *   the token was parsed
  *
  * This class is used by the `toTokens()` method in
  * [[grizzled.string.Implicits.String.GrizzledString]].
  *
  * @param string  the string token
  * @param start   the start of the token within the original string
  */
final case class StringToken(string: String, start: Int) {
  override def toString = string
}
