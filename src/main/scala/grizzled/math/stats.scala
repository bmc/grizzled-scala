package grizzled.math

/** Miscellaneous statistics-related functions.
  *
  * Note: You must import `scala.math.Numeric` (or just `Numeric._`) for these
  * functions to work. For example:
  *
  * {{{
  * import Numeric._
  * import grizzled.math.stats._
  *
  * val l = List[Double]( ... )
  * println(median(l))
  * }}}
  */
object stats {

  /** Calculates the geometric mean of the values of the passed-in
    * numbers, namely, the n-th root of (x1 * x2 * ... * xn). Note that
    * all numbers used in the calculation of a geometric mean must be
    * positive.
    *
    * For a discussion of when a geometric mean is more suitable than an
    * arithmetic mean, see
    * [[http://www.math.toronto.edu/mathnet/questionCorner/geomean.html]].
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the geometric mean
    */
  def geometricMean[T: Numeric](item: T, items: T*): Double = {
    val itemList = (item +: items).toList
    val n = implicitly[Numeric[T]]
    require (! itemList.exists(n.toDouble(_) <= 0))

    items match {
      case seq if seq.isEmpty =>
        n.toDouble(item)

      case _ =>
        val recip = 1.0 / itemList.length.toDouble
        itemList.foldLeft(1.0)((a, b) => a * math.pow(n.toDouble(b), recip))
    }
  }

  /** Calculates the harmonic mean of the values of the passed-in
    * numbers, namely: `n / (1/x^1^ + 1/x^2^ + ... + 1/x^n^).`
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the harmonic mean
    */
  def harmonicMean[T: Numeric](item: T, items: T*): Double = {
    val n = implicitly[Numeric[T]]

    items.toList match {
      case Nil => n.toDouble(item)
      case _ =>
        val allItems = item +: items
        allItems.length / allItems.foldLeft(0.0)((a, b) => a + (1.0 / n.toDouble(b)))
    }
  }

  /** Calculates the arithmetic mean of the values of the passed-in
    * numbers.
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the arithmetic mean
    */
  def arithmeticMean[T: Numeric](item: T, items: T*): Double = {
    val n = implicitly[Numeric[T]]

    items.toList match {
      case Nil => n.toDouble(item)
      case _ =>
        val allItems = item +: items
        allItems.foldLeft(0.0)((a, b) => a + n.toDouble(b)) / allItems.length
    }
  }

  /** Synonym for `arithmeticMean`.
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the arithmetic mean
    *
    * @see arithmeticMean
    */
  def mean[T: Numeric](item: T, items: T*): Double =
    arithmeticMean(item, items: _*)


  /** Calculates the median of the values of the passed-in numbers.
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the median
    */
  def median[T: Numeric](item: T, items: T*): Double = {
    val n = implicitly[Numeric[T]]

    if (items.length == 0)
      n.toDouble(item)

    else {
      val allItems = item +: items
      val sorted = allItems sortWith (n.compare(_, _) < 0)
      val midpoint = sorted.length / 2
      allItems.length % 2 match {
        case 0 => mean(n.toDouble(sorted(midpoint)),
                       n.toDouble(sorted(midpoint - 1)))
        case 1 => n.toDouble(sorted(midpoint))
      }
    }
  }

  /** Calculates the mode (most common value(s)) of the values of the
    * passed-in numbers. If there are multiple common values, they're all
    * returned.
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return list of modal values
    */
  def mode[T: Numeric](item: T, items: T*): List[T] = {
    if (items.length == 1)
      List(item)

    else {
       // Count the occurrences of each value. This is a reduceByKey operation.
      val allItems = item +: items
      val m = allItems
        .map(_ -> 1)
        .groupBy(_._1)
        .map { case (n, counts) => n -> counts.map(_._2).sum }

      // Find the maximum count.
      val max = m.values.max

      // Extract the keys with values that match
      m.filter { case (_, v) => v == max }.keys.toList
    }
  }

  /** Calculate the population variance of the finite population defined
    * by the `items` arguments. The population variance is defined as:
    *
    * {{{
    * 1
    * - *  SUM(i=1, N) { (x[i] - mean)^2^ }
    * N
    * }}}
    *
    * See:
    *
    * - [[http://en.wikipedia.org/wiki/Variance#Population_variance_and_sample_variance]]
    * - [[http://www.quickmba.com/stats/standard-deviation/]]
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the variance
    */
  def populationVariance[T: Numeric](item: T, items: T*): Double = {
    val allItems = item +: items
    calculateVariance(allItems.length, allItems.toList)
  }

  /** Calculate the unbiased sample variance of the finite sample defined
    * by the `items` arguments. The sample variance is defined as:
    *
    * {{{
    *   1
    * ----- *   SUM(i=1, N) { (x[i] - sampleMean)^2^  }
    * N - 1
    * }}}
    *
    * See:
    *
    * - [[http://en.wikipedia.org/wiki/Variance#Population_variance_and_sample_variance]]
    * - [[http://www.quickmba.com/stats/standard-deviation/]]
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the variance
    */
  def sampleVariance[T: Numeric](item: T, items: T*): Double = {
    val allItems = item +: items
    calculateVariance(allItems.length -1, allItems.toList)
  }

  /** Calculate the population standard deviation of the specified values.
    * The population standard deviation is merely the square root of the
    * population variance. Thus, this function is just shorthand for:
    *
    * {{{
    * java.lang.Math.sqrt(populationVariance(items))
    * }}}
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the standard deviation
    */
  def populationStandardDeviation[T: Numeric](item: T, items: T*): Double =
    java.lang.Math.sqrt(populationVariance(item, items: _*))

  /** Shorter synonym for `populationStandardDeviation`.
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the standard deviation
    */
  def popStdDev[T: Numeric](item: T, items: T*): Double =
    java.lang.Math.sqrt(populationVariance(item, items: _*))

  /** Calculate the sample standard deviation of the specified values.
    * The sample standard deviation is merely the square root of the
    * sample variance. Thus, this function is just shorthand for:
    *
    * {{{
    * java.lang.Math.sqrt(sampleVariance(items))
    * }}}
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the sample standard deviation
    */
  def sampleStandardDeviation[T: Numeric](item: T, items: T*): Double =
    java.lang.Math.sqrt(sampleVariance(item, items: _*))

  /** Shorter synonym for `sampleStandardDeviation`.
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the sample standard deviation
    *
    * @see populationStandardDeviation
    */
  def sampleStdDev[T: Numeric](item: T, items: T*): Double =
    java.lang.Math.sqrt(sampleVariance(item, items: _*))


  /** Calculate the range of a data set. This function does a single
    * linear pass over the data set.
    *
    * @param item  the first number on which to operate
    * @param items the remaining numbers on which to operate
    *
    * @return the range
    */
  def range[T: Numeric](item: T, items: T*): T = {
    val n = implicitly[Numeric[T]]
    items.length match {
      case 0 => n.minus(item, item)
      case _ =>
        // Fold left, generating a (min, max) tuple along the way.
        val allItems = item +: items
        val (min, max) =
          allItems.foldLeft((n.fromInt(Int.MaxValue), n.fromInt(0)))((tuple, i) =>
           (n.min(tuple._1, i), n.max(tuple._1, i)))
        n.minus(max, min)
    }
  }

  private def calculateVariance[T: Numeric](denominator: Int,
                                            items: Seq[T]): Double = {
    def sumOfSquares(dList: Seq[Double]): Double =
      dList.foldLeft(0.0) ((sum, d) => sum + (d * d))

    require (items.length > 1)
    val n = implicitly[Numeric[T]]

    items match {
      case seq if seq.isEmpty =>
        0
      case Seq(head, tail @ _*) =>
        val mn = mean(head, tail: _*)
        val deviations = items map (n.toDouble(_) - mn)
        sumOfSquares(deviations) / denominator.toDouble
    }
  }
}
