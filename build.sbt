scalaVersion := "2.13.8"

enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)

name := "openolitor-server"
mainClass := Some("ch.openolitor.core.Boot")
Compile / mainClass := Some("ch.openolitor.core.Boot")

assembly / assemblyJarName := "openolitor-server.jar"

assemblyMergeStrategy in assembly := {
  case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
  case "library.properties"                      => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import java.text.SimpleDateFormat
import java.util.Calendar


val specs2V = "4.15.0" // based on spray 1.3.x built in support
val akkaV = "2.6.+"
val sprayV = "1.3.+"
val scalalikeV = "4.0.0"
val akkaHttpVersion = "10.2.9"
val akkaVersion = "2.6.19"

resolvers += Resolver.typesafeRepo("releases")

val buildSettings = Seq(
  scalariformPreferences := scalariformPreferences.value
  .setPreference(DanglingCloseParenthesis, Force)
  .setPreference(AlignSingleLineCaseStatements, true),
  organization := "ch.openolitor.scalamacros",
  version := "2.5.12",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8", "2.11.11"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  // ### removed: resolvers += "Spray" at "http://repo.spray.io",
  resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  // add -Xcheckinit to scalac options to check for null val's during initialization see also: https://docs.scala-lang.org/tutorials/FAQ/initialization-order.html
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-language:_", "-language:postfixOps"),

  libraryDependencies ++= {
  Seq(
    "org.scala-lang.modules"     %% "scala-xml"                % "2.1.0",
    "javax.xml.bind"             %  "jaxb-api"                 % "2.3.1",
    "com.typesafe.akka"          %% "akka-http"                % akkaHttpVersion,
    "com.typesafe.akka"          %% "akka-http-caching"        % akkaHttpVersion,
    "com.typesafe.akka"          %% "akka-http-spray-json"     % akkaHttpVersion, // ### NO Scala 3
    "com.typesafe.akka"            %%  "akka-actor"    					              % akkaVersion, // ### Scala 3, experimental
    "com.typesafe.akka"            %%  "akka-persistence"                     % akkaVersion,
    "com.typesafe.akka"            %%  "akka-persistence-query"               % akkaVersion,
    "com.typesafe.akka"            %%  "akka-slf4j"    					              % akkaVersion,
    "com.typesafe.akka"            %%  "akka-stream"    					            % akkaVersion,
    "com.typesafe.akka"            %%  "akka-testkit"  			    	            % akkaVersion       % "test",
    "com.typesafe.akka"            %%  "akka-http-testkit"  			    	      % akkaHttpVersion   % "test",
    "com.typesafe.akka"            %%  "akka-stream-testkit"  			    	    % akkaVersion       % "test",
    "com.lightbend.akka"           %%  "akka-persistence-jdbc"    					  % "5.0.4",
    "com.github.dnvriend"          %%  "akka-persistence-inmemory" 		        % "2.5.15.2"     % "test", // from "https://github.com/OpenOlitor/openolitor-legacy-dependencies/raw/master/com.github.dnvriend/akka-persistence-inmemory_2.11/jars/akka-persistence-inmemory_2.11-1.0.5.jar", // ### NO Scala 3
    "org.specs2"                   %%  "specs2-core"   					              % specs2V     % "test", // ### Scala 3
    "org.specs2"                   %%  "specs2-mock"                          % specs2V     % "test",
    "org.specs2"                   %%  "specs2-junit"                         % specs2V     % "test",
    "org.specs2"                   %%  "specs2-scalacheck"                    % specs2V     % "test",
    "org.mockito"                  %%  "mockito-scala"                        % "1.17.7"   % "test",
    "org.scalaz" 		               %%  "scalaz-core"						              % "7.3.6", // ### Scala 3
    //use scala logging to log outside of the actor system
    "com.typesafe.scala-logging"   %%  "scala-logging"				                % "3.9.4", // ### Scala 3
    // use currently own fork, until PR was merged and a new release is available
    //"org.scalikejdbc"              %%  "scalikejdbc-async"                    % "0.9.+",
    //"com.github.mauricio"          %%  "mysql-async" 						              % "0.2.+", // ### NO Scala 3, NO Scala 2.13 => scalikejdbc-async is Scala 3 and 2.13, and supports mysql
    //
    "org.scalikejdbc"              %%  "scalikejdbc-async"                    % "0.15.0",
    "org.scalikejdbc" 	           %%  "scalikejdbc-config"				            % scalalikeV, // ### Scala 3
    "org.scalikejdbc"              %%  "scalikejdbc-test"                     % scalalikeV   % "test", // ### Scala 3
    "org.scalikejdbc" 	           %%  "scalikejdbc-syntax-support-macro"     % scalalikeV, // ### Scala 3
    "org.scalikejdbc" 	           %%  "scalikejdbc-joda-time"                % scalalikeV, // ### Scala 3
    "com.github.jasync-sql"        %   "jasync-mysql"                         % "2.0.+",
    "com.h2database"               %   "h2"                                   % "2.1.212"    % "test",
    "ch.qos.logback"  	           %   "logback-classic"    		  		        % "1.2.11",
    "org.mariadb.jdbc"	           %   "mariadb-java-client"                  % "3.0.4",
    "mysql"	                       %   "mysql-connector-java"                 % "8.0.29",
    // Libreoffice document API
    "org.apache.odftoolkit"        %   "simple-odf"					                  % "0.8.2-incubating" withSources(),
    "org.apache.odftoolkit"        %   "simple-odf"        					          % "0.8.2-incubating" withSources(),
    "com.scalapenos"               %%  "stamina-json"                         % "0.1.6", // ### NO Scala 3
    "net.virtual-void" %%  "json-lenses" % "0.6.2",
    // s3
    "com.amazonaws"                %   "aws-java-sdk-s3"                      % "1.12.213",
    "de.svenkubiak"                %   "jBCrypt"                              % "0.4.1",
    "com.github.daddykotex"        %% "courier"                               % "3.1.0", // ### Scala 3
    "com.github.nscala-time"       %%  "nscala-time"                          % "2.30.0", // ### Scala 3
    "com.github.blemale"           %% "scaffeine"                             % "5.1.2", // ### Scala 3
    "de.zalando"                   %% "beard"                                 % "0.3.3" exclude("ch.qos.logback", "logback-classic") from "https://github.com/OpenOlitor/beard/releases/download/0.3.3/beard_2.13-0.3.3.jar", // ### NO Scala 3, NO Scala 2.13
    // transitive dependencies of legacy de.zalando.beard
    "org.antlr"                    % "antlr4"                                 % "4.8-1",
    "io.monix"                     %% "monix"                                 % "3.4.0", // ### Scala 3
    "net.codecrete.qrbill"         % "qrbill-generator"                       % "2.4.3",
    "io.nayuki"                    % "qrcodegen"                              % "1.6.0",
    "org.apache.pdfbox"            % "pdfbox"                                 % "2.0.20",
    "org.apache.pdfbox"            % "pdfbox-parent"                          % "2.0.20" pomOnly(),
    "org.apache.xmlgraphics"       % "batik-transcoder"                       % "1.10",
    "org.apache.xmlgraphics"       % "batik-codec"                            % "1.9",
    "com.tegonal"                  %% "cf-env-config-loader"                  % "1.1.2", // ### NO Scala 3, NO Scala 2.13
    "com.eatthepath"               % "java-otp"                               % "0.2.0",
    "org.apache.pdfbox"            % "pdfbox-tools"                           % "2.0.2"
  )
},
  dependencyOverrides ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0"
  )
)

lazy val scalaxbSettings = Seq(
   scalaxbXsdSource in (Compile, scalaxb) := baseDirectory.value / "src" / "main" / "resources" / "xsd",
   scalaxbPackageName in (Compile, scalaxb) := "ch.openolitor.generated.xsd",
   scalaxbPackageNames in (Compile, scalaxb) := Map(uri("urn:iso:std:iso:20022:tech:xsd:camt.054.001.06") -> "ch.openolitor.generated.xsd.camt054_001_06",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:camt.054.001.04") -> "ch.openolitor.generated.xsd.camt054_001_04",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:pain.008.001.07") -> "ch.openolitor.generated.xsd.pain008_001_07",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:pain.008.001.02") -> "ch.openolitor.generated.xsd.pain008_001_02")
)

// lazy val akkaPersistenceSqlAsyncUri = uri("git://github.com/OpenOlitor/akka-persistence-sql-async#fix/scalikejdbc_version_with_timeout")
// lazy val akkaPersistenceSqlAsync = ProjectRef(akkaPersistenceSqlAsyncUri, "core")

// lazy val scalikejdbcAsyncForkUri = uri("git://github.com/OpenOlitor/scalikejdbc-async.git#dev/oneToManies21Traversable")
// lazy val scalikejdbcAsync = ProjectRef(scalikejdbcAsyncForkUri, "core")

// lazy val sprayJsonMacro = RootProject(uri("git://github.com/openolitor/spray-json-macros.git"))

lazy val macroSub = (project in file("macro")).settings(buildSettings,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)

lazy val main = (project in file(".")).enablePlugins(sbtscalaxb.ScalaxbPlugin).settings(buildSettings ++ scalaxbSettings ++ Seq(
    (sourceGenerators in Compile) += task[Seq[File]]{
      val dir = (sourceManaged in Compile).value
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
    mappings in (Compile, packageSrc) ++= (managedSources in Compile).value.map{ f =>
      // to merge generated sources into sources.jar as well
      (f, f.relativeTo((sourceManaged in Compile).value).get.getPath)
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
dockerBaseImage := "openjdk:8"
dockerExposedPorts ++= Seq(9003)

// the directories created, e.g. /var/log/openolitor-server, are created using user id 1000,
// but the default user starting the app has id 1001 which wouldn't have access to it
daemonUserUid in Docker := Some("1000")

val todayD = Calendar.getInstance.getTime
val today = new SimpleDateFormat("yyyyMMdd").format(todayD)

dockerEnvVars := Map("JAVA_OPTS" -> "-XX:+ExitOnOutOfMemoryError -Xms256m -Xmx3G -Dconfig.file=/etc/openolitor-server/application.conf -Dlogback.configurationFile=/etc/openolitor-server/logback.xml",
                     "application_buildnr" -> today)

Docker / maintainer := "OpenOlitor Team <info@openolitor.org>"

Docker / version := sys.env.get("GITHUB_REF").getOrElse(version.value + "_SNAPSHOT").split("/").last
