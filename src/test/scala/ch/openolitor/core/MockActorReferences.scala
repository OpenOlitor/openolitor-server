package ch.openolitor.core

import akka.actor.ActorRef
import akka.testkit.TestProbe

trait MockActorReferences extends ActorReferences with SystemConfigReference with ActorSystemReference {
  private lazy val mailServiceProbe = TestProbe()(system)
  override val mailService: ActorRef = mailServiceProbe.ref

  private val dbEvolutionActorProbe = TestProbe()(system)
  override val dbEvolutionActor: ActorRef = dbEvolutionActorProbe.ref

  private lazy val airbrakeNotifierProbe = TestProbe()(system)
  override val airbrakeNotifier: ActorRef = airbrakeNotifierProbe.ref

  private lazy val entityStoreProbe = TestProbe()(system)
  override val entityStore: ActorRef = entityStoreProbe.ref

  private lazy val eventStoreProbe = TestProbe()(system)
  override val eventStore: ActorRef = eventStoreProbe.ref

  private lazy val jobQueueServiceProbe = TestProbe()(system)
  override val jobQueueService: ActorRef = jobQueueServiceProbe.ref

  private lazy val reportSystemProbe = TestProbe()(system)
  override val reportSystem: ActorRef = reportSystemProbe.ref
}
