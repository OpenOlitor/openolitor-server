package ch.openolitor.core.db

import ch.openolitor.arbeitseinsatz.ArbeitseinsatzDBMappings
import ch.openolitor.buchhaltung.BuchhaltungDBMappings
import ch.openolitor.core.models.{ BaseEntity, BaseId, PersonId }
import ch.openolitor.core.repositories.BaseEntitySQLSyntaxSupport
import ch.openolitor.stammdaten.repositories.DefaultStammdatenWriteRepositoryComponent
import ch.openolitor.stammdaten.StammdatenDBMappings
import scalikejdbc.{ Binders, DB }
import ch.openolitor.core.repositories.EventPublishingImplicits._

trait WithWriteRepositories extends MockDBComponent with StammdatenDBMappings with BuchhaltungDBMappings with ArbeitseinsatzDBMappings with DefaultStammdatenWriteRepositoryComponent {

  implicit lazy val stammdatenRepositoryImplicit = stammdatenWriteRepository

  def insertEntity[E <: BaseEntity[I], I <: BaseId](entity: E)(implicit user: PersonId, syntaxSupport: BaseEntitySQLSyntaxSupport[E], binder: Binders[I]): Option[E] = {
    DB autoCommitSinglePublish { implicit session => implicit publisher =>
      stammdatenWriteRepository.insertEntity[E, I](entity)
    }
  }
}
