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
    * @param items the numbers on which to operate
    *
    * @return the geometric mean
    */
  def geometricMean[T](items: T*)(implicit n: Numeric[T]): Double = {
    val itemList = items.toList
    val len = itemList.length
    require (len > 0)
    require (! itemList.exists(n.toDouble(_) <= 0))

    len match {
      case 1 =>
        n.toDouble(itemList.head)

      case _ =>
        val recip = 1.0 / len.toDouble
        (1.0 /: itemList)((a, b) => a * math.pow(n.toDouble(b), recip))
    }
  }

  /** Calculates the harmonic mean of the values of the passed-in
    * numbers, namely: `n / (1/x^1^ + 1/x^2^ + ... + 1/x^n^).`
    *
    * @param items the numbers on which to operate
    *
    * @return the harmonic mean
    */
  def harmonicMean[T](items: T*)(implicit n: Numeric[T]): Double = {
    val itemList = items.toList
    val len = itemList.length
    require (len > 0)

    len match {
      case 1 => n.toDouble(itemList.head)
      case _ => len / (0.0 /: itemList)((a, b) => a + (1.0 / n.toDouble(b)))
    }
  }

  /** Calculates the arithmetic mean of the values of the passed-in
    * numbers.
    *
    * @param items the numbers on which to operate
    *
    * @return the arithmetic mean
    */
  def arithmeticMean[T](items: T*)(implicit n: Numeric[T]): Double = {
    val itemList = items.toList
    val len = itemList.length
    require (len > 0)

    len match {
      case 1 => n.toDouble(itemList.head)
      case _ => (0.0 /: itemList)((a, b) => a + n.toDouble(b)) / len
    }
  }

  /** Synonym for `arithmeticMean`.
    *
    * @see arithmeticMean
    */
  def mean[T](items: T*)(implicit n: Numeric[T]): Double =
    arithmeticMean(items.toList: _*)


  /** Calculates the median of the values of the passed-in numbers.
    *
    * @param items the numbers on which to operate
    *
    * @return the median
    */
  def median[T](items: T*)(implicit n: Numeric[T]): Double = {
    val itemList = items.toList
    val len = itemList.length
    require (len > 0)

    if (len == 1)
      n.toDouble(itemList.head)

    else {
      val sorted = itemList sortWith (n.compare(_, _) < 0)
      val midpoint = sorted.length / 2
      len % 2 match {
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
    * @param items the numbers on which to operate
    *
    * @return list of modal values
    */
  def mode[T](items: T*)(implicit n: Numeric[T]): List[T] = {
    val itemList = items.toList
    val len = itemList.length
    require (len > 0)

    if (len == 1)
      itemList take 1

    else {
       // Count the occurrences of each value. This is a reduceByKey operation.
      val m = itemList.map { n => n -> 1 }
                      .groupBy(_._1)
                      .map { case (n, counts) => n -> counts.map(_._2).sum }

      // Find the maximum count.
      val max = m.values.max

      // Extract the keys with values that match
      m.filter { case (k, v) => v == max }.keys.toList
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
    * @param items  the numbers on which to operate
    *
    * @return the variance
    */
  def populationVariance[T](items: T*)(implicit n: Numeric[T]): Double =
    calculateVariance(items.length, items.toList)

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
    * @param items  the numbers on which to operate
    *
    * @return the variance
    */
  def sampleVariance[T](items: T*)(implicit n: Numeric[T]): Double =
    calculateVariance(items.length - 1, items.toList)

  /** Calculate the population standard deviation of the specified values.
    * The population standard deviation is merely the square root of the
    * population variance. Thus, this function is just shorthand for:
    *
    * {{{
    * java.lang.Math.sqrt(populationVariance(items))
    * }}}
    *
    * @param items the numbers on which to operate
    *
    * @return the standard deviation
    */
  def populationStandardDeviation[T](items: T*)(implicit n: Numeric[T]): Double =
    java.lang.Math.sqrt(populationVariance(items.toList: _*))

  /** Shorter synonym for `populationStandardDeviation`.
    *
    * @see populationStandardDeviation
    */
  def popStdDev[T](items: T*)(implicit n: Numeric[T]): Double =
    java.lang.Math.sqrt(populationVariance(items.toList: _*))

  /** Calculate the sample standard deviation of the specified values.
    * The sample standard deviation is merely the square root of the
    * sample variance. Thus, this function is just shorthand for:
    *
    * {{{
    * java.lang.Math.sqrt(sampleVariance(items))
    * }}}
    *
    * @param items the numbers on which to operate
    *
    * @return the standard deviation
    */
  def sampleStandardDeviation[T](items: T*)(implicit n: Numeric[T]): Double =
    java.lang.Math.sqrt(sampleVariance(items.toList: _*))

  /** Shorter synonym for `sampleStandardDeviation`.
    *
    * @see populationStandardDeviation
    */
  def sampleStdDev[T](items: T*)(implicit n: Numeric[T]): Double =
    java.lang.Math.sqrt(sampleVariance(items.toList: _*))


  /** Calculate the range of a data set. This function does a single
    * linear pass over the data set.
    *
    * @param items the numbers on which to operate
    *
    * @return the range
    */
  def range[T](items: T*)(implicit n: Numeric[T]): T = {
    items.length match {
      case 1 => n.minus(items(0), items(0))
      case _ =>
        // Fold left, generating a (min, max) tuple along the way.
        val (min, max) =
          ((n.fromInt(Int.MaxValue), n.fromInt(0)) /: items)((tuple, i) =>
           (n.min(tuple._1, i), n.max(tuple._1, i)))
        n.minus(max, min)
    }
  }

  private def calculateVariance[T](denominator: Int, items: List[T])
                                  (implicit n: Numeric[T]): Double = {

    def sumOfSquares(dList: List[Double]): Double =
      (0.0 /: dList) ((sum, d) => sum + (d * d))

    val itemList = items.toList
    val len = itemList.length
    require (len > 1)

    val mn = mean(itemList: _*)
    val deviations = itemList map (n.toDouble(_) - mn)
    sumOfSquares(deviations) / denominator.toDouble
  }
}
