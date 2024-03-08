package ch.openolitor.core.filestore

import ch.openolitor.core.config.ModifyingSystemConfigReference
import ch.openolitor.core.filestore.WithInMemoryFileStore.initMinio
import com.typesafe.config.{ Config, ConfigValueFactory }
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import java.util.concurrent.atomic.AtomicInteger

//import org.testcontainers.containers.{ MinIOContainer => JavaMinIOContainer }
import org.testcontainers.containers.MinIOContainer

//class MinioIOContainer(dockerImageName: String) extends JavaMinIOContainer[MinioIOContainer](dockerImageName)

trait WithInMemoryFileStore extends ModifyingSystemConfigReference with BeforeAfterAll {
  self: Specification =>

  protected lazy val randomPort = WithInMemoryFileStore.getNextPort()

  override protected def modifyConfig(): Config =
    super
      .modifyConfig()
      .withValue("openolitor.test.s3.aws-endpoint", ConfigValueFactory.fromAnyRef(s"http://localhost:$randomPort"))
      .withValue("openolitor.test.s3.aws-access-key-id", ConfigValueFactory.fromAnyRef("accessKey1"))
      .withValue("openolitor.test.s3.aws-secret-access-key", ConfigValueFactory.fromAnyRef("verySecretKey1"))

  def initializeInMemoryFileStore(): Unit = {
    initMinio(randomPort)
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

  def initMinio(port: Integer) = {
    new MinIOContainer("minio/minio:RELEASE.2022-10-24T18-35-07Z")
      .withExposedPorts(port)
  }
}