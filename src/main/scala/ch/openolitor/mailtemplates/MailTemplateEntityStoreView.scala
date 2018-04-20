package ch.openolitor.mailtemplates

import akka.actor.{ ActorRef, ActorSystem, Props }
import ch.openolitor.core.SystemConfig
import ch.openolitor.core.db.ConnectionPoolContextAware
import ch.openolitor.core.domain._
import ch.openolitor.core.models.BaseId
import ch.openolitor.mailtemplates.repositories.{ DefaultMailTemplateWriteRepositoryComponent, MailTemplateWriteRepositoryComponent }

object MailTemplateEntityStoreView {
  def props(dbEvolutionActor: ActorRef)(implicit sysConfig: SystemConfig, system: ActorSystem): Props =
    Props(classOf[DefaultMailTemplateEntityStoreView], dbEvolutionActor, sysConfig, system)
}

class DefaultMailTemplateEntityStoreView(override val dbEvolutionActor: ActorRef, implicit val sysConfig: SystemConfig, implicit val system: ActorSystem)
  extends MailTemplateEntityStoreView with DefaultMailTemplateWriteRepositoryComponent

trait MailTemplateEntityStoreView
  extends EntityStoreView with MailTemplateEntityStoreViewComponent with ConnectionPoolContextAware {
  self: MailTemplateWriteRepositoryComponent =>

  override val module = "mailtemplate"
  override def initializeEntityStoreView(): Unit = {}
}

trait MailTemplateEntityStoreViewComponent extends EntityStoreViewComponent {
  val sysConfig: SystemConfig
  val system: ActorSystem

  override val updateService = MailTemplateUpdateService(sysConfig, system)
  override val insertService = MailTemplateInsertService(sysConfig, system)
  override val deleteService = MailTemplateDeleteService(sysConfig, system)
  override val aktionenService: EventService[PersistentEvent] = ignore()

  def ignore[A <: ch.openolitor.core.domain.PersistentEvent]() = new EventService[A] {
    override val handle = Map[A, Unit]()
  }
}
