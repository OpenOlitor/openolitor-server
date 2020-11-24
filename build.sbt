scalaVersion := "2.11.11"

enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)

name := "openolitor-server"
mainClass in Compile := Some("ch.openolitor.core.Boot")

assemblyJarName in assembly := "openolitor-server.jar"

mainClass in assembly := Some("ch.openolitor.core.Boot")

assemblyMergeStrategy in assembly := {
  case PathList("org", "slf4j", xs @ _*)         => MergeStrategy.first
  case "library.properties"                      => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import java.text.SimpleDateFormat
import java.util.Calendar


val specs2V = "2.4.17" // based on spray 1.3.x built in support
val akkaV = "2.4.+"
val sprayV = "1.3.+"
val scalalikeV = "3.1.+"

resolvers += Resolver.typesafeRepo("releases")

val buildSettings = Seq(
  scalariformPreferences := scalariformPreferences.value
  .setPreference(DanglingCloseParenthesis, Force)
  .setPreference(AlignSingleLineCaseStatements, true),
  organization := "ch.openolitor.scalamacros",
  version := "2.4.21",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8", "2.11.11"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += "Spray" at "http://repo.spray.io",
  resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  // add -Xcheckinit to scalac options to check for null val's during initialization see also: https://docs.scala-lang.org/tutorials/FAQ/initialization-order.html
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-Ywarn-unused-import", "-feature", "-language:_"),
  mainClass in (Compile, run) := Some("ch.openolitor.core.Boot"),

  libraryDependencies ++= {
  Seq(
    "io.spray"                     %%  "spray-can"     					              % sprayV,
    "io.spray"                     %%  "spray-caching"     					          % sprayV,
    "io.spray"                     %%  "spray-routing-shapeless2" 		        % sprayV,
    "io.spray"                     %%  "spray-testkit" 					              % sprayV  % "test",
    "io.spray" 			               %%  "spray-json"    					              % sprayV,
    "io.spray" 			               %%  "spray-client"  					              % sprayV,
    "com.wandoulabs.akka"          %%  "spray-websocket" 				              % "0.1.4",
    "com.typesafe.akka"            %%  "akka-actor"    					              % akkaV,
    "com.typesafe.akka"            %%  "akka-persistence"                     % akkaV,
    "com.typesafe.akka"            %%  "akka-persistence-query-experimental"  % akkaV,
    "com.typesafe.akka"            %%  "akka-slf4j"    					              % akkaV,
    "com.typesafe.akka"            %%  "akka-stream"    					              % akkaV,
    "com.typesafe.akka"            %%  "akka-testkit"  			    	            % akkaV       % "test",
    "com.github.dnvriend"          %%  "akka-persistence-inmemory" 		        % "1.0.5"     % "test" from "https://github.com/OpenOlitor/openolitor-legacy-dependencies/raw/master/com.github.dnvriend/akka-persistence-inmemory_2.11/jars/akka-persistence-inmemory_2.11-1.0.5.jar",
    "org.specs2"                   %%  "specs2-core"   					              % specs2V     % "test",
    "org.specs2"                   %%  "specs2-mock"                          % specs2V     % "test",
    "org.specs2"                   %%  "specs2-junit"                         % specs2V     % "test",
    "org.specs2"                   %%  "specs2-scalacheck"                    % specs2V     % "test",
    "org.mockito"                  %   "mockito-core"                         % "1.10.19"   % "test",
    "org.scalaz" 		               %%  "scalaz-core"						              % "7.1.8",
    //use scala logging to log outside of the actor system
    "com.typesafe.scala-logging"   %%  "scala-logging"				                % "3.1.0",
    //akka persistence journal driver
    //"com.okumin" 		               %%  "akka-persistence-sql-async" 	        % "0.5.1",
    // use currently own fork, until PR was merged and a new release is available
    //"org.scalikejdbc"              %%  "scalikejdbc-async"                    % "0.9.+",
    "com.github.mauricio"          %%  "mysql-async" 						              % "0.2.+",
    //
    "org.scalikejdbc" 	           %%  "scalikejdbc-config"				            % scalalikeV,
    "org.scalikejdbc"              %%  "scalikejdbc-test"                     % scalalikeV   % "test",
    "com.h2database"               %   "h2"                                   % "1.4.191"    % "test",
    "org.scalikejdbc" 	           %%  "scalikejdbc-syntax-support-macro"     % scalalikeV,
    "ch.qos.logback"  	           %   "logback-classic"    		  		        % "1.1.7",
    "org.mariadb.jdbc"	           %   "mariadb-java-client"                  % "2.7.5",
    // Libreoffice document API
    "org.apache.odftoolkit"        %   "simple-odf"					                  % "0.8.2-incubating" withSources(),
    "com.jsuereth"                 %%  "scala-arm"                            % "1.4",
    //simple websocket client
    "org.jfarcand"                 %   "wcs"                                  % "1.5",
    "com.scalapenos"               %%  "stamina-json"                         % "0.1.1",
    // s3
    "com.amazonaws"                %   "aws-java-sdk-s3"                      % "1.11.807",
    "de.svenkubiak"                %   "jBCrypt"                              % "0.4.1",
    "com.github.daddykotex"        %% "courier"                                % "3.0.1",
    "com.github.nscala-time"       %%  "nscala-time"                          % "2.16.0",
    "com.github.blemale"           %% "scaffeine"                             % "2.2.0",
    "de.zalando"                   %% "beard"                                 % "0.2.0" exclude("ch.qos.logback", "logback-classic") from "https://github.com/OpenOlitor/openolitor-legacy-dependencies/raw/master/de.zalando/beard_2.11/jars/beard_2.11-0.2.0.jar",
    // transitive dependencies of legacy de.zalando.beard
    "org.antlr"                    % "antlr4"                                 % "4.5.2",
    "io.monix"                     %% "monix"                                 % "2.1.0",
    "net.codecrete.qrbill"         % "qrbill-generator"                       % "2.4.3",
    "io.nayuki"                    % "qrcodegen"                              % "1.6.0",
    "org.apache.pdfbox"            % "pdfbox"                                 % "2.0.20",
    "org.apache.pdfbox"            % "pdfbox-parent"                          % "2.0.20" pomOnly(),
    "org.apache.xmlgraphics"       % "batik-transcoder"                       % "1.10",
    "org.apache.xmlgraphics"       % "batik-codec"                            % "1.9",
    "com.tegonal"                  %% "cf-env-config-loader"                  % "1.0.2",
    "com.eatthepath"               % "java-otp"                               % "0.2.0"
  )
}
)


lazy val scalaxbSettings = Seq(
   scalaxbXsdSource in (Compile, scalaxb) := baseDirectory.value / "src" / "main" / "resources" / "xsd",
   scalaxbPackageName in (Compile, scalaxb) := "ch.openolitor.generated.xsd",
   scalaxbPackageNames in (Compile, scalaxb) := Map(uri("urn:iso:std:iso:20022:tech:xsd:camt.054.001.06") -> "ch.openolitor.generated.xsd.camt054_001_06",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:camt.054.001.04") -> "ch.openolitor.generated.xsd.camt054_001_04",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:pain.008.001.07") -> "ch.openolitor.generated.xsd.pain008_001_07",
                                                    uri("urn:iso:std:iso:20022:tech:xsd:pain.008.001.02") -> "ch.openolitor.generated.xsd.pain008_001_02")
)

lazy val akkaPersistenceSqlAsyncUri = uri("git://github.com/OpenOlitor/akka-persistence-sql-async#fix/scalikejdbc_version_with_timeout")
lazy val akkaPersistenceSqlAsync = ProjectRef(akkaPersistenceSqlAsyncUri, "core")

lazy val scalikejdbcAsyncForkUri = uri("git://github.com/OpenOlitor/scalikejdbc-async.git#dev/oneToManies21Traversable")
lazy val scalikejdbcAsync = ProjectRef(scalikejdbcAsyncForkUri, "core")

lazy val sprayJsonMacro = RootProject(uri("git://github.com/openolitor/spray-json-macros.git"))

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
  )) dependsOn (macroSub, sprayJsonMacro, scalikejdbcAsync, akkaPersistenceSqlAsync)

lazy val root = (project in file("root")).settings(buildSettings).aggregate(macroSub, main, sprayJsonMacro)

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

maintainer in Docker := "OpenOlitor Team <info@openolitor.org>"
packageSummary in Docker := "Server Backend of the OpenOlitor Platform"

version in Docker := sys.env.get("GITHUB_REF").getOrElse(version.value + "_SNAPSHOT").split("/").last
