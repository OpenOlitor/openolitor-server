package ch.openolitor.core

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.testkit.{ RouteTestTimeout, Specs2RouteTest }
import org.apache.pekko.testkit.TestProbe
import ch.openolitor.core.db.WithInMemoryDatabase
import ch.openolitor.core.filestore.{ MockFileStoreComponent, WithInMemoryFileStore }
import ch.openolitor.core.models._
import ch.openolitor.core.util.WithInMemoryPdfGenerator
import ch.openolitor.util.parsing.{ FilterExpr, GeschaeftsjahrFilter, QueryFilter }
import com.typesafe.scalalogging.LazyLogging
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck

import scala.concurrent.duration.DurationInt

trait BaseSpec extends Specification with ScalaCheck with Matchers {
  val defaultTimeout = 120 seconds
}

trait BaseRoutesSpec extends BaseSpec with Specs2RouteTest with SprayJsonSupport with AkkaHttpDeserializers with BaseJsonProtocol with EntityStoreReference with MockFileStoreComponent with MockActorReferences with LazyLogging {
  import org.apache.pekko.testkit._

  implicit val timeout = RouteTestTimeout(defaultTimeout.dilated)

  implicit val duration = 10 seconds
}

/**
 * Extending this base spec will initialize a docker container using in-memory storage, see [[ch.openolitor.core.db.WithInMemoryDatabase]]
 * and spinning up all the actors, see [[ch.openolitor.core.MockInMemoryActorReferences]].
 *
 * The database container is initialized once per class loader.
 * Each spec class will have its own database with a random identifier within this container.
 *
 * The database part is designed to support pumping the snapshot for each test case in [[org.specs2.specification.Before#before()]].
 * This snapshot is created once per spec.
 * As you can see the default behaviour here is to pump it once in [[org.specs2.specification.BeforeAll#beforeAll()]].
 * This is due to the fact that the actors are also created once in [[org.specs2.specification.BeforeAll#beforeAll()]].
 *
 * Feel free to extend this class and implement a reset of both the database and actors in [[org.specs2.specification.Before#before()]] to have a clean state for each test case.
 */
trait BaseRoutesWithDBSpec extends BaseRoutesSpec with WithInMemoryPdfGenerator with WithInMemoryFileStore with WithInMemoryDatabase with StartingServices with MockInMemoryActorReferences with EventMatchers with SpecSubjects with WsInteractionsForReports {
  sequential

  var dbEventProbe: TestProbe = null

  implicit protected val emptyFilter: Option[FilterExpr] = None
  implicit protected val emptyGjFilter: Option[GeschaeftsjahrFilter] = None
  implicit protected val emptyQueryString: Option[QueryFilter] = None

  override def beforeAll() = {
    initializeInMemoryDatabase()

    initializeConnectionPool()

    initializeInMemoryFileStore()

    MockInMemoryActorReferences.initialize(config, sysConfig)

    dbEventProbe = initializeDBEventProbe()
  }

  /**
   * @return a [[akka.testkit.TestProbe]] listening on the eventStream for [[ch.openolitor.core.models.DBEvent]]s.
   */
  protected def initializeDBEventProbe() = {
    val probe = TestProbe()
    MockInMemoryActorReferences.MockStartedServices(sysConfig).app.eventStream.subscribe(probe.ref, classOf[DBEvent[_]])
    probe
  }
}