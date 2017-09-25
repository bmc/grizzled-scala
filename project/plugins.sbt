addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC12")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
      url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
          Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

// Not built for 2.12. Can't use it yet.
//addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.14")

