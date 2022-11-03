package ch.openolitor.core.filestore

import akka.actor.ActorSystem
import ch.openolitor.core.config.ModifyingSystemConfigReference
import com.typesafe.config.{ Config, ConfigValueFactory }
import io.findify.s3mock.S3Mock
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import java.util.concurrent.atomic.AtomicInteger

trait WithInMemoryFileStore extends ModifyingSystemConfigReference with BeforeAfterAll {
  self: Specification =>

  implicit protected val system: ActorSystem
  protected lazy val randomPort = WithInMemoryFileStore.getNextPort()
  protected lazy val api = S3Mock(port = randomPort)

  override protected def modifyConfig(): Config =
    super
      .modifyConfig()
      .withValue("openolitor.test.s3.aws-endpoint", ConfigValueFactory.fromAnyRef(s"http://localhost:$randomPort"))
      .withValue("openolitor.test.s3.aws-access-key-id", ConfigValueFactory.fromAnyRef("accessKey1"))
      .withValue("openolitor.test.s3.aws-secret-access-key", ConfigValueFactory.fromAnyRef("verySecretKey1"))

  def initializeInMemoryFileStore(): Unit = {
    api.start
  }

  override def beforeAll(): Unit = {
    initializeInMemoryFileStore()
  }
}

object WithInMemoryFileStore {
  val currentPort = new AtomicInteger(9051)

  def getNextPort() = {
    currentPort.incrementAndGet()
  }
}