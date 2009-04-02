package grizzled

/**
 * Useful binary-related utility functions.
 */
object binary
{
    /**
     * Count the number of bits in a numeric (integer or long) value. This
     * method is adapted from the Hamming Weight algorithm. It works for
     * up to 64 bits.
     *
     * @param num  the numeric value
     *
     * @return the number of 1 bits in a binary representation of <tt>num</tt>
     */
    def bitCount(num: Int): Int =
    {
        val numLong: Long = num.toLong
        bitCount(numLong & 0xffffffffl)
    }

    /**
     * Count the number of bits in a numeric (integer or long) value. This
     * method is adapted from the Hamming Weight algorithm. It works for
     * up to 64 bits.
     *
     * @param num  the numeric value
     *
     * @return the number of 1 bits in a binary representation of <tt>num</tt>
     */
    def bitCount(num: Long): Int =
    {
        // Put count of each 2 bits into those 2 bits.
        val res1: Long = num - ((num >> 1) & 0x5555555555555555l)

        // Put count of each 4 bits into those 4 bits.
        val allThrees = 0x3333333333333333l
        val res2 = (res1 & allThrees) + ((res1 >> 2) & allThrees)

        // Put count of each 8 bits into those 8 bits.
        val res3 = (res2 + (res2 >> 4)) & 0x0f0f0f0f0f0f0f0fl

        // Left-most bits.
        ((res3 * 0x0101010101010101l) >> 56) toInt
    }
}
