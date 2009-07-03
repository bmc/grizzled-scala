/*---------------------------------------------------------------------------*\
  This software is released under a BSD-style license:

  Copyright (c) 2009 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

  1.  Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

  2.  The end-user documentation included with the redistribution, if any,
      must include the following acknowlegement:

        "This product includes software developed by Brian M. Clapper
        (bmc@clapper.org, http://www.clapper.org/bmc/). That software is
        copyright (c) 2009 Brian M. Clapper."

      Alternately, this acknowlegement may appear in the software itself,
      if wherever such third-party acknowlegements normally appear.

  3.  Neither the names "clapper.org", "The Grizzled Scala Library",
      nor any of the names of the project contributors may be used to
      endorse or promote products derived from this software without prior
      written permission. For written permission, please contact
      bmc@clapper.org.

  4.  Products derived from this software may not be called "clapper.org
      Java Utility Library", nor may "clapper.org" appear in their names
      without prior written permission of Brian M. Clapper.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
  NO EVENT SHALL BRIAN M. CLAPPER BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
\*---------------------------------------------------------------------------*/

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

