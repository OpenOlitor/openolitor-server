package ch.openolitor.core

import org.apache.pekko.testkit.TestProbe
import ch.openolitor.core.models._
import com.typesafe.scalalogging.LazyLogging
import org.specs2.matcher.MatchResult
import org.specs2.SpecificationLike

import scala.reflect.ClassTag

trait EventMatchers extends SpecificationLike with LazyLogging {
  var dbEventProbe: TestProbe

  protected def expectDBEvents(amountOfEvents: Int)(events: (Seq[EntityCreated[_]], Seq[EntityModified[_]], Seq[EntityDeleted[_]], Seq[DataEvent[_]]) => MatchResult[_]) = {
    val messages = dbEventProbe.receiveN(amountOfEvents).map(_.asInstanceOf[DBEvent[_]])

    val creations = messages.filter(_.isInstanceOf[EntityCreated[_]]).map(_.asInstanceOf[EntityCreated[_]])
    val modifications = messages.filter(_.isInstanceOf[EntityModified[_]]).map(_.asInstanceOf[EntityModified[_]])
    val deletions = messages.filter(_.isInstanceOf[EntityDeleted[_]]).map(_.asInstanceOf[EntityDeleted[_]])
    val dataEvents = messages.filter(_.isInstanceOf[DataEvent[_]]).map(_.asInstanceOf[DataEvent[_]])

    logDBEvents("creations", creations)
    logDBEvents("modifications", modifications)
    logDBEvents("deletions", deletions)
    logDBEvents("dataEvents", dataEvents)

    events(creations, modifications, deletions, dataEvents)
  }

  protected def oneEventMatches[E <: BaseEntity[_ <: BaseId]](events: Seq[DBEvent[_]])(assertion: E => MatchResult[Any])(implicit classTag: ClassTag[E]) = {
    events.filter(_.entity.getClass == classTag.runtimeClass).filter(element => assertion(element.entity.asInstanceOf[E]).isSuccess) must not be empty
  }

  protected def allEventsMatch[E <: BaseEntity[_ <: BaseId]](events: Seq[DBEvent[_]])(assertion: E => MatchResult[Any])(implicit classTag: ClassTag[E]) = {
    events.filter(_.entity.getClass == classTag.runtimeClass).forall(element => assertion(element.entity.asInstanceOf[E]).isSuccess) === true
  }

  protected def oneOf[E <: BaseEntity[_ <: BaseId]](events: Seq[DBEvent[_]])(predicate: E => Boolean)(implicit classTag: ClassTag[E]): E = {
    events.filter(_.entity.getClass == classTag.runtimeClass).filter(element => predicate(element.entity.asInstanceOf[E])).head.entity.asInstanceOf[E]
  }

  private def logDBEvents(name: String, events: Seq[DBEvent[_]]): Unit = {
    val prefix = s"DBEvent/$name: "
    logger.debug(s"$prefix${events.size}")
    if (events.size > 0) {
      events.foreach(e => logger.debug(s"$prefix$e"))
    }
  }
}
