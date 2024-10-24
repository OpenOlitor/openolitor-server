scalaVersion := "2.13.13"

enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

name := "openolitor-server"
mainClass := Some("ch.openolitor.core.Boot")
Compile / mainClass := Some("ch.openolitor.core.Boot")

assembly / assemblyJarName := "openolitor-server.jar"

assembly / assemblyMergeStrategy := {
  case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
  case "library.properties"                      => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.packager.docker.Cmd

import java.text.SimpleDateFormat
import java.util.Calendar


val specs2V = "4.20.8" // based on spray 1.3.x built in support
val akkaV = "2.7.+"

val sprayV = "1.3.+"
val scalalikeV = "4.3.1"
val akkaHttpVersion = "10.5.3"
val akkaVersion = "2.8.5"
val testContainersVersion = "1.20.1"

resolvers += Resolver.typesafeRepo("releases")

val buildSettings = Seq(
  scalariformPreferences := scalariformPreferences.value
  .setPreference(DanglingCloseParenthesis, Force)
  .setPreference(AlignSingleLineCaseStatements, true),
  version := "2.6.40",
  scalaVersion := "2.13.13",
  crossScalaVersions := Seq("2.13.8", "2.13.13"),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  resolvers ++= Resolver.sonatypeOssRepos("releases"),
  resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  // add -Xcheckinit to scalac options to check for null val's during initialization see also:
  // https://docs.scala-lang.org/tutorials/FAQ/initialization-order.html
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-language:_", "-language:postfixOps"),

  libraryDependencies ++= {
  Seq(
    "org.scala-lang.modules"       %% "scala-xml"                          % "2.3.0",
    "javax.xml.bind"               %  "jaxb-api"                           % "2.3.1",
    "com.typesafe.akka"            %% "akka-http"                          % akkaHttpVersion,
    "com.typesafe.akka"            %% "akka-http-caching"                  % akkaHttpVersion,
    "com.typesafe.akka"            %% "akka-http-spray-json"               % akkaHttpVersion, // ### NO Scala 3
    "com.typesafe.akka"            %% "akka-actor"                         % akkaVersion,
    "com.typesafe.akka"            %% "akka-persistence"                   % akkaVersion,
    "com.typesafe.akka"            %% "akka-persistence-query"             % akkaVersion,
    "com.typesafe.akka"            %% "akka-slf4j"    					           % akkaVersion,
    "com.typesafe.akka"            %% "akka-stream"    					           % akkaVersion,
    "com.typesafe.akka"            %% "akka-testkit"  			    	         % akkaVersion                             % "test",
    "com.typesafe.akka"            %% "akka-http-testkit"  			    	     % akkaHttpVersion                         % "test",
    "com.typesafe.akka"            %% "akka-stream-testkit"  			    	   % akkaVersion                             % "test",
    "com.lightbend.akka"           %% "akka-persistence-jdbc"    					 % "5.0.4",
    "com.github.dnvriend"          %% "akka-persistence-inmemory" 		     % "2.5.15.2"                              % "test", // ### NO Scala 3
    "org.specs2"                   %% "specs2-core"   					           % specs2V                                 % "test", // ### Scala 3
    "org.specs2"                   %% "specs2-mock"                        % specs2V                                 % "test",
    "org.specs2"                   %% "specs2-junit"                       % specs2V                                 % "test",
    "org.specs2"                   %% "specs2-scalacheck"                  % specs2V                                 % "test",
    "org.mockito"                  %% "mockito-scala"                      % "1.17.37"                                % "test",
    "org.scalaz" 		               %% "scalaz-core"						             % "7.3.6", // ### Scala 3
    //use scala logging to log outside of the actor system
    "com.typesafe.scala-logging"   %% "scala-logging"				               % "3.9.5", // ### Scala 3
    "org.scalikejdbc"              %% "scalikejdbc-async"                  % "0.20.0",
    "org.scalikejdbc" 	           %% "scalikejdbc-config"				         % scalalikeV, // ### Scala 3
    "org.scalikejdbc"              %% "scalikejdbc-test"                   % scalalikeV                              % "test", // ### Scala 3
    "org.scalikejdbc" 	           %% "scalikejdbc-syntax-support-macro"   % scalalikeV, // ### Scala 3
    "org.scalikejdbc" 	           %% "scalikejdbc-joda-time"              % scalalikeV, // ### Scala 3
    "com.github.jasync-sql"        %  "jasync-mysql"                       % "2.2.+",
    "com.h2database"               %  "h2"                                 % "2.3.232"                               % "test",
    "org.testcontainers"           %  "mariadb"                            % testContainersVersion                   % "test",
    "io.findify"                   %% "s3mock"                             % "0.2.6"                                 % "test",
    "ch.qos.logback"  	           %  "logback-classic"    		  		       % "1.5.8",
    "org.mariadb.jdbc"	           %  "mariadb-java-client"                % "3.1.4",
    "com.mysql"	                       %  "mysql-connector-j"               % "9.0.0",
    // Libreoffice document API
    "org.odftoolkit"               %  "simple-odf"					               % "0.9.0" withSources(),
    "com.scalapenos"               %% "stamina-json"                       % "0.1.6", // ### NO Scala 3
    "net.virtual-void"             %% "json-lenses"                        % "0.6.2",
    // s3
    "com.amazonaws"                %  "aws-java-sdk-s3"                    % "1.12.770",
    "de.svenkubiak"                %  "jBCrypt"                            % "0.4.1",
    "com.github.daddykotex"        %% "courier"                            % "3.2.0", // ### Scala 3
    "com.github.nscala-time"       %% "nscala-time"                        % "2.32.0", // ### Scala 3
    "com.github.blemale"           %% "scaffeine"                          % "5.3.0", // ### Scala 3
    "de.zalando"                   %% "beard"                              % "0.3.3" exclude("ch.qos.logback", "logback-classic") from "https://github.com/OpenOlitor/beard/releases/download/0.3.3/beard_2.13-0.3.3.jar", // ### NO Scala 3, NO Scala 2.13
    // transitive dependencies of legacy de.zalando.beard
    "org.antlr"                    %  "antlr4"                             % "4.8-1",
    "io.monix"                     %% "monix"                              % "3.4.1", // ### Scala 3
    "net.codecrete.qrbill"         %  "qrbill-generator"                   % "3.2.0",
    "io.nayuki"                    %  "qrcodegen"                          % "1.8.0",
    "org.apache.pdfbox"            %  "pdfbox"                             % "2.0.32",
    "org.apache.pdfbox"            %  "pdfbox-parent"                      % "2.0.32" pomOnly(),
    "org.apache.xmlgraphics"       %  "batik-transcoder"                   % "1.17",
    "org.apache.xmlgraphics"       %  "batik-codec"                        % "1.17",
    "com.tegonal"                  %% "cf-env-config-loader"               % "1.1.2", // ### NO Scala 3, NO Scala 2.13
    "com.eatthepath"               %  "java-otp"                           % "0.4.0",
    "org.apache.pdfbox"            %  "pdfbox-tools"                       % "2.0.32"
  )
},
  dependencyOverrides ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1",
    "xerces" % "xercesImpl" % "2.12.2",
    "org.apache.commons" % "commons-compress" % "1.26.2",
    "io.netty" % "netty-handler" % "4.1.107.Final",
    "org.apache.jena" % "jena-core" % "4.6.1",
    "com.google.protobuf" % "protobuf-java" % "3.21.10",
    "com.google.guava" % "guava" % "33.0.0-jre"
  )
)

lazy val scalaxbSettings = Seq(
   Compile / scalaxb / scalaxbXsdSource := baseDirectory.value / "src" / "main" / "resources" / "xsd",
   Compile / scalaxb / scalaxbPackageName := "ch.openolitor.generated.xsd",
   Compile / scalaxb / scalaxbPackageNames  := Map(uri("urn:iso:std:iso:20022:tech:xsd:camt.054.001.06") -> "ch.openolitor.generated.xsd.camt054_001_06",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:camt.054.001.04") -> "ch.openolitor.generated.xsd.camt054_001_04",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:pain.008.001.07") -> "ch.openolitor.generated.xsd.pain008_001_07",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:pain.008.001.02") -> "ch.openolitor.generated.xsd.pain008_001_02")
)

lazy val macroSub = (project in file("macro")).settings(buildSettings,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)

lazy val coverageSettings = Seq(
  coverageExcludedPackages := "$:;<empty>;ch.openolitor.core.Boot;.*Default.*;scalaxb.*;ch.openolitor.generated.*;ch.openolitor.core.scalax.*;ch.openolitor.core.repositories.Parameters*"
)

lazy val testSettings = Seq(
  Test / parallelExecution := true,
  Test / fork := true,
  Test / testForkedParallel := true,
  testOptions += Tests.Argument("timefactor", "5"),
  Test / javaOptions ++= Seq(
    "-Dconfig.resource=application-test.conf"
  )
)

lazy val main = (project in file(".")).enablePlugins(sbtscalaxb.ScalaxbPlugin).settings(buildSettings ++ scalaxbSettings ++ coverageSettings ++ testSettings ++ Seq(
  Compile / sourceGenerators += task[Seq[File]]{
      val dir = (Compile / sourceManaged).value
      val maxParams = 30
      val mappings = (1 to maxParams).map{ n =>
        val file = dir / "openolitor" / "ch" / "openolitor" / "core" / "repositories" / s"Parameters${n}.scala"
        IO.write(file, GenerateParametersMapping(n))
        file
      }
      val paramsTrait = {
        val file = dir / "openolitor" / "ch" / "openolitor" / "core" / "repositories" / s"Parameters.scala"
        IO.write(file, GenerateParametersTrait(maxParams))
        file
      }
      val tuples = (23 to maxParams).map{ n =>
        val file = dir / "openolitor" / "ch" / "openolitor" / "core" / "scalax" / s"Tuple${n}.scala"
        IO.write(file, GenerateTuples(n))
        file
      }
      mappings ++ tuples :+ paramsTrait
    },
  Compile / packageSrc / mappings ++= (Compile / managedSources).value.map{ f =>
      // to merge generated sources into sources.jar as well
    (f, f.relativeTo((Compile / sourceManaged).value).get.getPath)
    }
  )) dependsOn (macroSub)

lazy val root = (project in file("root")).settings(buildSettings).aggregate(macroSub, main)

dockerUsername := Some("openolitor")

val updateLatest = sys.env.get("DOCKER_UPDATE_LATEST") match {
                        case Some("true") =>
                            true
                        case _ =>
                            false
                      }

dockerUpdateLatest := updateLatest
dockerBaseImage := "eclipse-temurin:21-alpine"
dockerExposedPorts ++= Seq(9003)

// the directories created, e.g. /var/log/openolitor-server, are created using user id 1000,
// but the default user starting the app has id 1001 which wouldn't have access to it
Docker / daemonUserUid := Some("1000")

val todayD = Calendar.getInstance.getTime
val today = new SimpleDateFormat("yyyyMMdd").format(todayD)

dockerEnvVars := Map("application_buildnr" -> today)

Docker / maintainer := "OpenOlitor Team <info@openolitor.org>"

Docker / version := sys.env.get("GITHUB_REF").getOrElse(version.value + "_SNAPSHOT").split("/").last
