import org.scalatest.FunSuite

class GrizzledFunSuite extends FunSuite
{
    override def expect(expected: Any)(actual: => Any) 
    {
        try
        {
            super.expect(expected)(actual)
        }
        catch
        {
            case ex: java.lang.Throwable =>
                ex.printStackTrace()
                System.exit(1)
        }
    }

    override def expect(expected: Any, message: Any)(actual: => Any) 
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

