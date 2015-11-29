// Placeholder


addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
      url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
          Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

