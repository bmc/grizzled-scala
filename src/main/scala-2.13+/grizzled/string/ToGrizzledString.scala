package grizzled.string

import scala.language.implicitConversions
import scala.collection.immutable.WrappedString
import grizzled.string.Implicits.String.GrizzledString

abstract class ToGrizzledString {

  implicit def WrappedStringToGrizzledString(rs: WrappedString): GrizzledString =
    new GrizzledString(rs.toString)

  implicit def StringBuilderToGrizzledString(rs: scala.StringBuilder): GrizzledString =
    new GrizzledString(rs.toString)

}
