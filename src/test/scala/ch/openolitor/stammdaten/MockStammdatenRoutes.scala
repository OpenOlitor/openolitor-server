package ch.openolitor.stammdaten

import akka.actor.ActorSystem
import ch.openolitor.buchhaltung.repositories.{ DefaultBuchhaltungReadRepositoryAsyncComponent, MockBuchhaltungReadRepositoryComponent }
import ch.openolitor.core.filestore.MockFileStoreComponent
import ch.openolitor.core.{ MockActorReferences, MockInMemoryActorReferences, SystemConfig }
import ch.openolitor.core.db.MockDBComponent
import ch.openolitor.stammdaten.repositories.{ DefaultStammdatenReadRepositoryAsyncComponent, MockStammdatenReadRepositoryComponent }

import scala.concurrent.ExecutionContext

class MockStammdatenRoutes(override val sysConfig: SystemConfig, override val system: ActorSystem)(override implicit protected val executionContext: ExecutionContext) extends StammdatenRoutes
  with DefaultStammdatenReadRepositoryAsyncComponent
  with DefaultBuchhaltungReadRepositoryAsyncComponent
  with MockFileStoreComponent
  with MockInMemoryActorReferences
  with MockDBComponent {
}
