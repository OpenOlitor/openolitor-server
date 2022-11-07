package ch.openolitor.arbeitseinsatz

import akka.actor.ActorSystem
import ch.openolitor.arbeitseinsatz.repositories.DefaultArbeitseinsatzReadRepositoryAsyncComponent
import ch.openolitor.core.{ MockInMemoryActorReferences, SystemConfig }
import ch.openolitor.core.db.MockDBComponent
import ch.openolitor.core.filestore.MockFileStoreComponent
import ch.openolitor.stammdaten.repositories.DefaultStammdatenReadRepositoryAsyncComponent

import scala.concurrent.ExecutionContext

class MockArbeitseinsatzRoutes(override val sysConfig: SystemConfig, override val system: ActorSystem)(override implicit protected val executionContext: ExecutionContext) extends ArbeitseinsatzRoutes
  with DefaultStammdatenReadRepositoryAsyncComponent
  with DefaultArbeitseinsatzReadRepositoryAsyncComponent
  with MockFileStoreComponent
  with MockInMemoryActorReferences
  with MockDBComponent {
}