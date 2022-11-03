package ch.openolitor.core.util

import ch.openolitor.core.config.ModifyingSystemConfigReference
import com.typesafe.config.{ Config, ConfigValueFactory }
import org.specs2.mutable.Specification
import org.testcontainers.containers.GenericContainer

class PdfGeneratorContainer(dockerImageName: String) extends GenericContainer[PdfGeneratorContainer](dockerImageName)

trait WithInMemoryPdfGenerator extends ModifyingSystemConfigReference {
  self: Specification =>

  protected lazy val pdfGenerator = WithInMemoryPdfGenerator.initPdfGenerator

  override protected def modifyConfig(): Config =
    super
      .modifyConfig()
      .withValue("openolitor.test.converttopdf.endpoint", ConfigValueFactory.fromAnyRef(s"http://localhost:${pdfGenerator.getMappedPort(8080)}/lool/convert-to/pdf"))
}

object WithInMemoryPdfGenerator {
  protected lazy val initPdfGenerator = {
    val pdfGenerator = new PdfGeneratorContainer("eugenmayer/jodconverter:rest")
      .withExposedPorts(8080)

    pdfGenerator.start()

    pdfGenerator
  }
}