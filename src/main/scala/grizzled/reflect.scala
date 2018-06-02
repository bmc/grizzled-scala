package grizzled

/** Some reflection-related utility methods and classes.
  */
object reflect {
  import scala.reflect.{ClassTag, classTag}

  /** Determine whether an object is of a particular type. Example
    * of use:
    *
    * {{{
    * def foo(obj: Any) = {
    *   // Is this object of type Seq[Int] or just Int?
    *   if (isOfType[Int](obj))
    *     ...
    *   else if (isOfType[Seq[Int]](obj))
    *     ...
    *   else
    *     ...
    * }
    * }}}
    *
    * @param  o  the object to test
    * @tparam T   the type to test against
    *
    * @return `true` if `o` is of type `T`, `false` if not.
    */
  def isOfType[T: ClassTag](o: Any): Boolean = {
    val clsT = classTag[T].runtimeClass

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def isPrimitive[P: ClassTag]: Boolean =
      classTag[P].runtimeClass.isAssignableFrom(o.asInstanceOf[AnyRef].getClass)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def isClass: Boolean =
      clsT.isAssignableFrom(o.asInstanceOf[AnyRef].getClass)

    clsT.toString match {
      case "int"      => isPrimitive[java.lang.Integer]
      case "short"    => isPrimitive[java.lang.Short]
      case "long"     => isPrimitive[java.lang.Long]
      case "float"    => isPrimitive[java.lang.Float]
      case "double"   => isPrimitive[java.lang.Double]
      case "char"     => isPrimitive[java.lang.Character]
      case "byte"     => isPrimitive[java.lang.Byte]
      case "boolean"  => isPrimitive[java.lang.Boolean]
      case _          => isClass
    }
  }
}
