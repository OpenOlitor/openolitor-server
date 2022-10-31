package ch.openolitor.core

import akka.testkit.TestProbe
import ch.openolitor.core.models._
import org.specs2.matcher.MatchResult
import org.specs2.SpecificationLike

import scala.reflect.ClassTag

trait EventMatchers extends SpecificationLike {
  var dbEventProbe: TestProbe

  protected def expectDBEvents(amountOfEvents: Int)(events: (Seq[EntityCreated[_]], Seq[EntityModified[_]], Seq[EntityDeleted[_]], Seq[DataEvent[_]]) => MatchResult[_]) = {
    val messages = dbEventProbe.receiveN(amountOfEvents).map(_.asInstanceOf[DBEvent[_]])

    events(
      messages.filter(_.isInstanceOf[EntityCreated[_]]).map(_.asInstanceOf[EntityCreated[_]]),
      messages.filter(_.isInstanceOf[EntityModified[_]]).map(_.asInstanceOf[EntityModified[_]]),
      messages.filter(_.isInstanceOf[EntityDeleted[_]]).map(_.asInstanceOf[EntityDeleted[_]]),
      messages.filter(_.isInstanceOf[DataEvent[_]]).map(_.asInstanceOf[DataEvent[_]])
    )
  }

  protected def oneEventMatches[E <: BaseEntity[_ <: BaseId]](events: Seq[DBEvent[_]])(assertion: E => MatchResult[Any])(implicit classTag: ClassTag[E]) = {
    events.filter(_.entity.getClass == classTag.runtimeClass).filter(element => assertion(element.entity.asInstanceOf[E]).isSuccess) must not be empty
  }

  protected def allEventsMatch[E <: BaseEntity[_ <: BaseId]](events: Seq[DBEvent[_]])(assertion: E => MatchResult[Any])(implicit classTag: ClassTag[E]) = {
    events.filter(_.entity.getClass == classTag.runtimeClass).forall(element => assertion(element.entity.asInstanceOf[E]).isSuccess) === true
  }
}
