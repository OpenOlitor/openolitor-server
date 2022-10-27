package ch.openolitor.core

import akka.actor.ActorRef
import ch.openolitor.core.MockInMemoryActorReferences.MockStartedServices
import com.typesafe.config.Config

import scala.collection.concurrent.TrieMap

trait MockInMemoryActorReferences extends MockActorReferences {
  override lazy val mailService: ActorRef = MockStartedServices(sysConfig).mailService

  override lazy val dbEvolutionActor: ActorRef = MockStartedServices(sysConfig).dbEvolutionActor

  override lazy val airbrakeNotifier: ActorRef = MockStartedServices(sysConfig).airbrakeNotifier

  override lazy val entityStore: ActorRef = MockStartedServices(sysConfig).entityStore

  override lazy val eventStore: ActorRef = MockStartedServices(sysConfig).eventStore

  override lazy val jobQueueService: ActorRef = MockStartedServices(sysConfig).jobQueueService

  override lazy val reportSystem: ActorRef = MockStartedServices(sysConfig).reportSystem
}

object MockInMemoryActorReferences extends StartingServices {
  private val jdbcUrlToServices = TrieMap.empty[String, StartedServices]

  def MockStartedServices(systemConfig: SystemConfig) = {
    jdbcUrlToServices(systemConfig.mandantConfiguration.config.getString("db.default.url"))
  }

  def initialize(baseConfig: Config, systemConfig: SystemConfig) = {
    val services = startServicesForConfiguration(baseConfig, systemConfig.mandantConfiguration)

    jdbcUrlToServices.getOrElseUpdate(systemConfig.mandantConfiguration.config.getString("db.default.url"), services)
  }
}