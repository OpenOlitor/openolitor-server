package ch.openolitor.kundenportal

import akka.actor.ActorSystem
import ch.openolitor.core.{ MockInMemoryActorReferences, SystemConfig }
import ch.openolitor.core.db.MockDBComponent
import ch.openolitor.core.filestore.MockFileStoreComponent
import ch.openolitor.kundenportal.repositories.DefaultKundenportalReadRepositoryAsyncComponent

import scala.concurrent.ExecutionContext

class MockKundenportalRoutes(override val sysConfig: SystemConfig, override val system: ActorSystem)(override implicit protected val executionContext: ExecutionContext) extends KundenportalRoutes
  with DefaultKundenportalReadRepositoryAsyncComponent
  with MockFileStoreComponent
  with MockInMemoryActorReferences
  with MockDBComponent {
}
