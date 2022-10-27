package ch.openolitor.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.testkit.{ RouteTestTimeout, Specs2RouteTest }
import ch.openolitor.core.db.WithInMemoryDatabase
import ch.openolitor.core.filestore.MockFileStoreComponent
import com.typesafe.scalalogging.LazyLogging
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck

import scala.concurrent.duration.DurationInt

trait BaseSpec extends Specification with ScalaCheck with Matchers {
  val defaultTimeout = 120 seconds
}

trait BaseRoutesSpec extends BaseSpec with Specs2RouteTest with SprayJsonSupport with AkkaHttpDeserializers with BaseJsonProtocol with EntityStoreReference with MockFileStoreComponent with MockActorReferences with LazyLogging {
  import akka.testkit._

  implicit val timeout = RouteTestTimeout(defaultTimeout.dilated)
}

trait BaseRoutesWithDBSpec extends BaseRoutesSpec with WithInMemoryDatabase with StartingServices with MockInMemoryActorReferences {
  override def beforeAll() = {
    super.beforeAll()

    initializeConnectionPool()

    MockInMemoryActorReferences.initialize(config, sysConfig)
  }
}