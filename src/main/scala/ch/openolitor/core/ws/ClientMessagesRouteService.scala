package ch.openolitor.core.ws

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.SourceQueueWithComplete
import ch.openolitor.core.{ BaseRouteService, SystemConfig }
import ch.openolitor.core.filestore.DefaultFileStoreComponent
import ch.openolitor.core.models.PersonId
import ch.openolitor.core.security.{ Subject, XSRFTokenSessionAuthenticatorProvider }

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext

trait ClientMessagesRouteService extends BaseRouteService with XSRFTokenSessionAuthenticatorProvider {
  val clientMessagesService: ClientMessagesService

  val routes: Route =
    extractRequestContext { requestContext => // secured routes by XSRF token authenticator
      authenticate(requestContext) { implicit subject =>
        path("ws") {
          handleWebSocketMessages(clientMessagesService.handler)
        }
      }
    }
}

class DefaultClientMessagesRouteService(
  override val entityStore: ActorRef,
  override val sysConfig: SystemConfig,
  override val system: ActorSystem,
  override val loginTokenCache: Cache[String, Subject],
  val streamsByUser: TrieMap[PersonId, scala.collection.concurrent.Map[String, SourceQueueWithComplete[String]]]
) extends ClientMessagesRouteService
  with DefaultFileStoreComponent {
  override implicit protected val executionContext: ExecutionContext = system.dispatcher
  override val clientMessagesService = new DefaultClientMessagesService(system, loginTokenCache, streamsByUser)
  override val maxRequestDelay = Some(20 seconds)
}
