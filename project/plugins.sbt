addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.1")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.12.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
//addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.5.10")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.2")

dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.4.0"
