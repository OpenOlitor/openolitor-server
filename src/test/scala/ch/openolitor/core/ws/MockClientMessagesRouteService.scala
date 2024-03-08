package ch.openolitor.core.ws

import org.apache.pekko.actor.ActorSystem
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.core.{ MockInMemoryActorReferences, SpecSubjects, SystemConfig }
import ch.openolitor.core.db.MockDBComponent
import ch.openolitor.core.filestore.MockFileStoreComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryAsyncComponent

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.DurationInt

class MockClientMessagesRouteService(override val sysConfig: SystemConfig, override val system: ActorSystem)(override implicit protected val executionContext: ExecutionContext) extends ClientMessagesRouteService
  with DefaultStammdatenReadRepositoryAsyncComponent
  with DefaultBuchhaltungReadRepositoryAsyncComponent
  with MockFileStoreComponent
  with MockInMemoryActorReferences
  with MockDBComponent
  with SpecSubjects {

  override lazy val loginTokenCache = MockInMemoryActorReferences.MockStartedServices(sysConfig).loginTokenCache
  override lazy val clientMessagesService = new DefaultClientMessagesService(system, loginTokenCache, {
    MockInMemoryActorReferences.MockStartedServices(sysConfig).loginTokenCache.put(adminSubjectToken, Future.successful(adminSubject))
    MockInMemoryActorReferences.MockStartedServices(sysConfig).streamsByUser
  })
  override val maxRequestDelay = Some(20 seconds)
}
