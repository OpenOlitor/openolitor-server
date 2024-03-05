package ch.openolitor.stammdaten

import ch.openolitor.core.MailServiceReference

trait MailCommandForwarderComponent {
  val mailCommandForwarder: MailCommandForwarder
}

trait DefaultMailCommandForwarderComponent extends MailCommandForwarderComponent with MailServiceReference {
  override lazy val mailCommandForwarder = MailCommandForwarder(mailService)
}
