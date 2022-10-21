package ch.openolitor.stammdaten.repositories

import org.specs2.mock.Mockito

trait MockStammdatenReadRepositoryComponent extends StammdatenReadRepositoryAsyncComponent with Mockito {
  override val stammdatenReadRepository: StammdatenReadRepositoryAsync = mock[StammdatenReadRepositoryAsync]
}
