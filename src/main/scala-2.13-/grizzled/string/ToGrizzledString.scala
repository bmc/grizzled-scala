package grizzled.string

import scala.language.implicitConversions
import scala.collection.immutable.StringLike
import grizzled.string.Implicits.String.GrizzledString

abstract class ToGrizzledString {

  implicit def String_GrizzledString(rs: StringLike[String]): GrizzledString =
    new GrizzledString(rs.toString)

}
