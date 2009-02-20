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
}

