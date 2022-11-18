package ch.openolitor.core.filestore

import akka.actor.ActorSystem
import ch.openolitor.core.MandantConfiguration

import scala.collection.concurrent.TrieMap

trait MockFileStoreComponent extends FileStoreComponent {
  override lazy val fileStore = MockFileStoreComponent.MockFileStore(sysConfig.mandantConfiguration, system)
}

object MockFileStoreComponent {
  private val s3UrlToFileStore = TrieMap.empty[String, S3FileStore]

  def MockFileStore(mandantConfiguration: MandantConfiguration, actorSystem: ActorSystem) = {
    s3UrlToFileStore.getOrElseUpdate(mandantConfiguration.config.getString("s3.aws-access-key-id"), S3FileStore(mandantConfiguration, actorSystem))
  }
}
