package grizzled

/** Compatibility definitions for Scala 2.13+ vs. Scala 2.12 and lesser.
  * This object is conceptually similar to `scala.collection.compat`.
  *
  * - For Scala 2.12 and earlier, it provides a type alias and compatibility
  *   functions for `LazyList`. For Scala 2.13 and greater, it's empty. Thus,
  *   all code can use `LazyList` throughout.
  * - It also provides the implicit objects `Ordering` objects for floats and
  *   doubles. For instance, it provides
  *   `grizzled.ScalaCompat.math.Ordering.Double.IeeeOrdering` and
  *   `grizzled.ScalaCompat.math.Ordering.Double.IeeeOrdering`. For Scala 2.12
  *   and earlier, these values are aliases for `scala.math.Ordering.Double`.
  *   For Scala 2.13 and greater, they map to their 2.13 counterparts (e.g.,
  *   `scala.math.Ordering.Double.IeeeOrdering`).
  */
package object ScalaCompat {
  import scala.collection.convert.{DecorateAsJava, DecorateAsScala}

  val CollectionConverters: DecorateAsJava with DecorateAsScala =
    scala.collection.JavaConverters

  type LazyList[+T] = Stream[T]

  object LazyList {
    def empty[T]: LazyList[T] = Stream.empty[T]

    object #:: {
      @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
      def unapply[T](s: LazyList[T]): Option[(T, LazyList[T])] =
        if (s.nonEmpty) Some((s.head, s.tail)) else None
    }
  }

  object math {
    object Ordering {
      object Double {
        implicit val IeeeOrdering: Ordering[Double] =
          scala.math.Ordering.Double
        implicit val TotalOrdering: Ordering[Double] =
          scala.math.Ordering.Double
      }
      object Float {
        implicit val IeeeOrdering: Ordering[Float] =
          scala.math.Ordering.Float
        implicit val TotalOrdering: Ordering[Float] =
          scala.math.Ordering.Float
      }
    }
  }

  object scalautil {
    import scala.util.Try
    import scala.util.control.{ControlThrowable, NonFatal}

    // Stolen directly from
    // https://github.com/scala/scala/blob/2.13.x/src/library/scala/util/Using.scala
    // See that file and the official API docs for details.
    //
    // Doesn't include everything in the 2.13 Using. Includes enough to make
    //
    object Using {
      trait Releasable[-R] {
        def release(resource: R): Unit
      }

      object Releasable {
        implicit object AutoCloseableIsReleasable extends Releasable[AutoCloseable] {
          def release(resource: AutoCloseable): Unit = resource.close()
        }
      }

      def apply[R: Releasable, A](resource: => R)(f: R => A): Try[A] = {
        Try {
          Using.resource(resource)(f)
        }
      }
      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf",
                              "org.wartremover.warts.Throw",
                              "org.wartremover.warts.Var",
                              "org.wartremover.warts.Null"))
      def resource[R, A](resource: R)(body: R => A)
                        (implicit releaseable: Releasable[R]): A = {
        if (resource == null) throw new NullPointerException("null resource")

        var toThrow: Throwable = null
        try {
          body(resource)
        }
        catch {
          case t: Throwable =>
            toThrow = t
            null.asInstanceOf[A]
        }
        finally {
          if (toThrow eq null) {
            releaseable.release(resource)
          }
          else {
            try {
              releaseable.release(resource)
            }
            catch {
              case other: Throwable =>
                toThrow = preferentiallySuppress(toThrow, other)
            }
            finally {
              throw toThrow
            }
          }
        }
      }
    }

    private def preferentiallySuppress(primary: Throwable, secondary: Throwable): Throwable = {
      def score(t: Throwable): Int = t match {
        case _: VirtualMachineError                   => 4
        case _: LinkageError                          => 3
        case _: InterruptedException | _: ThreadDeath => 2
        case _: ControlThrowable                      => 0
        case e if !NonFatal(e)                        => 1 // in case this method gets out of sync with NonFatal
        case _                                        => -1
      }
      @inline def suppress(t: Throwable, suppressed: Throwable): Throwable = {
        t.addSuppressed(suppressed)
        t
      }

      if (score(secondary) > score(primary)) suppress(secondary, primary)
      else suppress(primary, secondary)
    }
  }
}
