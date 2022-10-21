package ch.openolitor.buchhaltung.repositories

import org.specs2.mock.Mockito

trait MockBuchhaltungReadRepositoryComponent extends BuchhaltungReadRepositoryAsyncComponent with Mockito {
  override val buchhaltungReadRepository: BuchhaltungReadRepositoryAsync = mock[BuchhaltungReadRepositoryAsync]
}
