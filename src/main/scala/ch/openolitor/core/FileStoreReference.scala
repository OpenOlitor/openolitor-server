package ch.openolitor.core

import ch.openolitor.core.filestore.FileStore

trait FileStoreReference {
  protected val fileStore: FileStore
}
