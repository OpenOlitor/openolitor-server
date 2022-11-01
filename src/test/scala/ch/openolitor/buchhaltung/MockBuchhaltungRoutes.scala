package ch.openolitor.buchhaltung

import akka.actor.ActorSystem
import ch.openolitor.buchhaltung.repositories.DefaultBuchhaltungReadRepositoryAsyncComponent
import ch.openolitor.core.{ MockInMemoryActorReferences, SystemConfig }
import ch.openolitor.core.db.MockDBComponent
import ch.openolitor.core.filestore.MockFileStoreComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryAsyncComponent

import scala.concurrent.ExecutionContext

class MockBuchhaltungRoutes(override val sysConfig: SystemConfig, override val system: ActorSystem)(override implicit protected val executionContext: ExecutionContext) extends BuchhaltungRoutes
  with DefaultStammdatenReadRepositoryAsyncComponent
  with DefaultBuchhaltungReadRepositoryAsyncComponent
  with MockFileStoreComponent
  with MockInMemoryActorReferences
  with MockDBComponent {
}
