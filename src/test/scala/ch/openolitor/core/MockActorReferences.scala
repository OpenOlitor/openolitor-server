package ch.openolitor.core

import akka.actor.ActorRef
import akka.testkit.TestProbe

trait MockActorReferences extends ActorReferences with SystemConfigReference with ActorSystemReference {
  private lazy val mailServiceProbe = TestProbe()(system)
  override lazy val mailService: ActorRef = mailServiceProbe.ref

  private lazy val dbEvolutionActorProbe = TestProbe()(system)
  override lazy val dbEvolutionActor: ActorRef = dbEvolutionActorProbe.ref

  private lazy val airbrakeNotifierProbe = TestProbe()(system)
  override lazy val airbrakeNotifier: ActorRef = airbrakeNotifierProbe.ref

  private lazy val entityStoreProbe = TestProbe()(system)
  override lazy val entityStore: ActorRef = entityStoreProbe.ref

  private lazy val eventStoreProbe = TestProbe()(system)
  override lazy val eventStore: ActorRef = eventStoreProbe.ref

  private lazy val jobQueueServiceProbe = TestProbe()(system)
  override lazy val jobQueueService: ActorRef = jobQueueServiceProbe.ref

  private lazy val reportSystemProbe = TestProbe()(system)
  override lazy val reportSystem: ActorRef = reportSystemProbe.ref
}
