import scala.reflect.Manifest
import org.scalatest.{FunSuite, Assertions}

/**
 * A version of ScalaTest's FunSuite that actually aborts the running test
 * if an <tt>expect()</tt> or an <tt>intercept()</tt> fails.
 */
class GrizzledFunSuite extends FunSuite
{
    override def intercept[T <: AnyRef](f: => Any)
                                       (implicit manifest: Manifest[T]): T =
    {
        try
        {
            super.intercept(f)(manifest)
        }

        catch
        {
            case ex: java.lang.Throwable =>
                ex.printStackTrace()
                System.exit(1)
                throw ex // not reached, but keeps Scala happy
        }
    }

    override def expect(expected: Any, message: Any)(actual: Any) 
    {
        try
        {
            super.expect(expected, message)(actual)
        }
        catch
        {
            case ex: java.lang.Throwable =>
                ex.printStackTrace()
                System.exit(1)
        }
    }

    /**
     * Sets the specified values in the system properties, runs the
     * the specified code block, and restores the environment.
     *
     * @param code  code block to run
     */
    def withProperties(properties: Map[String, String])(code: => Any)
    {
        import scala.collection.mutable

        val old = mutable.Map[String, String]()
        for ((key, value) <- properties)
        {
            val oldValue = 
                if (System.getProperty(key) == null)
                    ""
                else
                    System.getProperty(key)
            
            old += key -> oldValue
            System.setProperty(key, value)
        }

        try
        {
            code
        }

        finally
        {
            for ((key, value) <- old)
                System.setProperty(key, value)
        }
    }
}

