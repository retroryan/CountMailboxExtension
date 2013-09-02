resolvers += Classpaths.typesafeResolver

resolvers += "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo"

addSbtPlugin("com.typesafe.akka" % "akka-sbt-plugin" % "2.2.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-atmos" % "0.1.1")

addSbtPlugin("reaktor" % "sbt-scct" % "0.2-SNAPSHOT")

