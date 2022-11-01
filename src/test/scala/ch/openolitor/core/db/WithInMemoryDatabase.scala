package ch.openolitor.core.db

import ch.openolitor.core.config.ModifyingSystemConfigReference
import ch.openolitor.core.db.evolution.{ DBEvolutionActor, Evolution }
import ch.openolitor.core.{ MandantConfiguration, SystemConfig }
import ch.openolitor.core.mailservice.ActorTestScope
import com.typesafe.config.{ Config, ConfigValueFactory }
import com.typesafe.scalalogging.LazyLogging
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import org.testcontainers.containers.{ MariaDBContainer => JavaMariaDBContainer }
import org.testcontainers.utility.MountableFile

import java.util.Collections
import akka.testkit.TestProbe
import akka.util.Timeout
import ch.openolitor.core.db.evolution.scripts.Scripts
import ch.openolitor.core.db.evolution.DBEvolutionActor.{ CheckDBEvolution, DBEvolutionState }
import ch.openolitor.stammdaten.models.KundeId
import com.tegonal.CFEnvConfigLoader.ConfigLoader
import scalikejdbc.ConnectionPool

import scala.util.{ Failure, Random, Success, Try }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

// Define adapter class to match strange self type of testcontainers
class MariaDBContainer(dockerImageName: String) extends JavaMariaDBContainer[MariaDBContainer](dockerImageName)

/**
 * Creates an in memory database which is unique per jdbcUrl and runs in the same `MariaDBContainer`. As all the
 * components used in the test class use the same config, they will be using the same mock database identified by the
 * jdbcUrl.
 */
trait WithInMemoryDatabase extends ModifyingSystemConfigReference with BeforeAfterAll with MockDBComponent {
  self: Specification =>
  // run each test using WithInMemoryDatabase sequential as we use the same database instance for all tests within a spec
  // but re-pump template database to the same database for every test case
  sequential

  override protected def modifyConfig(): Config = {
    val url = ConfigValueFactory.fromAnyRef(
      s"${mariaDB.getJdbcUrl.replaceAll("mariadb", "mysql").replaceAll("replace_me", dbName)}?user=tegonal&password=tegonal&autoReconnect=true"
    )
    val driver = ConfigValueFactory.fromAnyRef("com.mysql.cj.jdbc.Driver")
    val user = ConfigValueFactory.fromAnyRef("tegonal")
    val password = ConfigValueFactory.fromAnyRef("tegonal")

    // we have to setup slick.db.* and db.default.*
    super
      .modifyConfig()
      .withValue("openolitor.try.slick.db.url", url)
      .withValue("openolitor.try.slick.db.driver", driver)
      .withValue("openolitor.try.slick.db.user", user)
      .withValue("openolitor.try.slick.db.password", password)
      .withValue("openolitor.try.jdbc-journal.slick.db.url", url)
      .withValue("openolitor.try.jdbc-journal.slick.db.driver", driver)
      .withValue("openolitor.try.jdbc-journal.slick.db.user", user)
      .withValue("openolitor.try.jdbc-journal.slick.db.password", password)
      .withValue("openolitor.try.jdbc-snapshot-store.slick.db.url", url)
      .withValue("openolitor.try.jdbc-snapshot-store.slick.db.driver", driver)
      .withValue("openolitor.try.jdbc-snapshot-store.slick.db.user", user)
      .withValue("openolitor.try.jdbc-snapshot-store.slick.db.password", password)
      .withValue("openolitor.try.jdbc-read-journal.slick.db.url", url)
      .withValue("openolitor.try.jdbc-read-journal.slick.db.driver", driver)
      .withValue("openolitor.try.jdbc-read-journal.slick.db.user", user)
      .withValue("openolitor.try.jdbc-read-journal.slick.db.password", password)
      .withValue("openolitor.try.db.default.url", url)
      .withValue("openolitor.try.db.default.driver", driver)
      .withValue("openolitor.try.db.default.user", user)
      .withValue("openolitor.try.db.default.password", password)
  }

  protected lazy val dbName = WithInMemoryDatabase.getAndRegisterUniqueDbName()

  lazy val mariaDB = WithInMemoryDatabase.initSnapshotSql

  def initializeInMemoryDatabase(): Unit = {
    val result = mariaDB.execInContainer(s"./mock_db_pump.sh", s"$dbName")

    if (result.getExitCode != 0) {
      throw new IllegalStateException(s"Failed to pump into $dbName ${result.getStderr}")
    }
  }

  override def beforeAll(): Unit = {
    initializeInMemoryDatabase()
  }
}

object WithInMemoryDatabase extends ActorTestScope with LazyLogging {
  private def createSet[T]() = {
    import scala.jdk.CollectionConverters._
    java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap[T, java.lang.Boolean]).asScala
  }

  // keep track of all database names acquired in the same test container/vm to guarantee unique db names within a spec
  val usedDBNames = createSet[String]()

  def getAndRegisterUniqueDbName(): String = synchronized {
    val dbName = generateDbName()
    if (!usedDBNames.contains(dbName)) {
      usedDBNames += dbName
      dbName
    } else {
      // recursively try again
      getAndRegisterUniqueDbName()
    }
  }

  private def generateDbName() = s"oo_${Random.alphanumeric.filter(c => c > 96 && c < 123).take(12).mkString}"

  private lazy val baseConfig = ConfigLoader.loadConfig

  protected lazy val initSnapshotSql = {
    val mariaDB = new MariaDBContainer("mariadb:10.7")
      .withDatabaseName("replace_me")
      .withTmpFs(Collections.singletonMap("/var/lib/mysql", "rw"))
      .withCommand(
        "--character-set-server=utf8mb4",
        "--collation-server=utf8mb4_unicode_ci",
        "--max_connections=10000",
        "--default-time-zone=+00:00"
      )
      .withUsername("tegonal")
      .withPassword("tegonal")

    mariaDB.start()

    // add scripts to container
    mariaDB.copyFileToContainer(MountableFile.forClasspathResource("db_schema.sql"), "db_schema.sql")
    mariaDB.copyFileToContainer(MountableFile.forClasspathResource("mock_db_schema_init.sh"), "mock_db_schema_init.sh")
    mariaDB.copyFileToContainer(MountableFile.forClasspathResource("mock_db_dump.sh"), "mock_db_dump.sh")
    mariaDB.copyFileToContainer(MountableFile.forClasspathResource("mock_db_pump.sh"), "mock_db_pump.sh")

    mariaDB.execInContainer("./mock_db_schema_init.sh")

    executeEvolutions(mariaDB.getJdbcUrl.replaceAll("mariadb", "mysql"))

    mariaDB.execInContainer("./mock_db_dump.sh")

    mariaDB
  }

  private def executeEvolutions(jdbcUrl: String): Unit = {
    implicit val timeout: Timeout = Timeout(50.seconds)
    implicit val executionContext: ExecutionContext = system.dispatcher
    val url = ConfigValueFactory.fromAnyRef(s"$jdbcUrl?user=tegonal&password=tegonal&autoReconnect=true")
    val driver = ConfigValueFactory.fromAnyRef("com.mysql.cj.jdbc.Driver")
    val user = ConfigValueFactory.fromAnyRef("tegonal")
    val password = ConfigValueFactory.fromAnyRef("tegonal")
    val config = baseConfig
      .withValue("slick.db.url", url)
      .withValue("slick.db.driver", driver)
      .withValue("slick.db.user", user)
      .withValue("slick.db.password", password)
      .withValue("db.default.url", url)
      .withValue("db.default.driver", driver)
      .withValue("db.default.user", user)
      .withValue("db.default.password", password)

    // inline temporary config to connect to the jdbcUrl separately
    ConnectionPool.singleton(config.getString("db.default.url"), "tegonal", "tegonal")

    val mandant = MandantConfiguration("", "", "", 0, 0, Map(classOf[KundeId] -> 30000), config)
    val connectionPoolContext = MandantDBs(mandant).connectionPoolContext()
    val asyncConnectionPoolContext = AsyncMandantDBs(mandant).connectionPoolContext()
    implicit val sysCfg = SystemConfig(mandant, connectionPoolContext, asyncConnectionPoolContext)

    val evolution = new Evolution(sysCfg, Scripts.current(system))
    val dbEvolutionActor = system.actorOf(DBEvolutionActor.props(evolution), "db-evolution")

    val probe = TestProbe()
    probe.send(dbEvolutionActor, CheckDBEvolution)

    val result = probe.expectMsgType[Try[DBEvolutionState]](60 seconds)

    result match {
      case Success(rev) =>
        logger.debug(s"Successfully check db with revision:$rev")
      case Failure(e) =>
        logger.warn(s"db evolution failed", e)
    }
  }
}
