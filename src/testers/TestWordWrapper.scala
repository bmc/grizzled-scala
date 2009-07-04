// Run this as a script.

import grizzled.string._

val s = "This is the Grizzled Scala API, which is similar to the clapper.org Grizzled Python API, only for Scala. (Duh.) It contains a variety of miscellaneous utility classes and objects. Basically, whenever I find myself writing something that's general-purpose, I put it in here, so I can easily use it in multiple projects.\n\nThis is after a blank line."

for (width <- List(79, 78, 50, 10))
{
    println("\n*** Without a prefix, width=" + width)
    println("|" + ("-" * (width - 2)) + "|")
    val w = new WordWrapper(width)
    println(w.wrap(s))

    println("\n*** With a prefix, width=" + width)
    println("|" + ("-" * (width - 2)) + "|")
    val prefix = "foo: "
    val w2 = new WordWrapper(width, 0, prefix)
    println(w2.wrap(s))

    println("\n*** With indentation, width=" + width)
    println("|" + ("-" * (width - 2)) + "|")
    val w3 = new WordWrapper(width, 4)
    println(w3.wrap(s))

    println("\n*** With indentation, using indent char '.', width=" + width)
    println("|" + ("-" * (width - 2)) + "|")
    val w4 = new WordWrapper(width, 4, "", '.')
    println(w4.wrap(s))
}

