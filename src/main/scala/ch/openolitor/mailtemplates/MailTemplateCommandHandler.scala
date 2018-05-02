package ch.openolitor.mailtemplates

import akka.actor.ActorSystem
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain.EntityStore.{ InsertEntityCommand, ResultingEvent }
import ch.openolitor.core.domain.{ CommandHandler, EventTransactionMetadata, IdFactory, UserCommand }
import ch.openolitor.core.models.EntityDeleted
import ch.openolitor.mailtemplates.model.{ MailTemplateId, MailTemplateModify }
import ch.openolitor.mailtemplates.repositories.{ DefaultMailTemplateReadRepositoryComponent, MailTemplateDBMappings, MailTemplateReadRepositorySync, MailTemplateReadRepositorySyncImpl }

import scala.util.Try

trait MailTemplateCommandHandler extends CommandHandler with MailTemplateDBMappings with ConnectionPoolContextAware {
  self: MailTemplateReadRepositorySync =>

  override val handle: PartialFunction[UserCommand, IdFactory => EventTransactionMetadata => Try[Seq[ResultingEvent]]] = {
    case e @ InsertEntityCommand(personIdd, entity: MailTemplateModify) => idFactory => meta =>
      handleEntityInsert[MailTemplateModify, MailTemplateId](idFactory, meta, entity, MailTemplateId.apply)
  }
}

class DefaultMailTemplateCommandHanlder(val sysConfig: SystemConfig, val system: ActorSystem)
  extends MailTemplateCommandHandler with MailTemplateReadRepositorySyncImpl {

}