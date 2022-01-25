package automorph.transport.http.server

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.settings.ServerSettings
import akka.util.Timeout
import automorph.Types
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.endpoint.AkkaHttpEndpoint
import automorph.transport.http.endpoint.AkkaHttpEndpoint.RpcHttpRequest
import automorph.transport.http.server.AkkaServer.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.Await

/**
 * Akka HTTP server transport plugin.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://doc.akka.io/docs/akka-http Library documentation]]
 * @see [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
 * @constructor Creates an Akka HTTP server with specified RPC request handler.
 * @param handler RPC request handler
 * @param port port to listen on for HTTP connections
 * @param pathPrefix HTTP URL path prefix, only requests starting with this path prefix are allowed
 * @param methods allowed HTTP request methods
 * @param mapException maps an exception to a corresponding HTTP status code
 * @param requestTimeout HTTP request processing timeout
 * @param serverSettings HTTP server settings
 * @tparam Effect effect type
 */
final case class AkkaServer[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  port: Int,
  pathPrefix: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  requestTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS),
  serverSettings: ServerSettings = AkkaServer.defaultServerSettings
) extends Logging with ServerMessageTransport[Effect] {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system
  private val actorSystem = start()

  override def close(): Effect[Unit] = {
    system.wrap {
      actorSystem.terminate()
      Await.result(actorSystem.whenTerminated, Duration.Inf)
      ()
    }
  }

  private def start(): ActorSystem[Nothing] = {
    val rootBehavior = Behaviors.setup[Nothing] { actorContext =>
      // Create handler actor
      implicit val actorSystem: ActorSystem[Nothing] = actorContext.system
      val handlerBehavior = AkkaHttpEndpoint.behavior(handler)
      val handlerActor = actorContext.spawn(handlerBehavior, handlerBehavior.getClass.getName)
      actorContext.watch(handlerActor)

      // Create HTTP route
      val route = AkkaHttpEndpoint.route(handlerActor, pathPrefix, methods, requestTimeout)

      // Start HTTP server
      val serverBinding = Await.result(
        Http().newServerAt("0.0.0.0", port).withSettings(serverSettings).bind(route),
        Duration.Inf
      )
      logger.info("Listening for connections", ListMap(
        "Protocol" -> Protocol.Http,
        "Port" -> serverBinding.localAddress.getPort.toString
      ))
      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}

object AkkaServer {

  /** Request context type. */
  type Context = AkkaHttpEndpoint.Context

  /** Default HTTP server settings. */
  def defaultServerSettings: ServerSettings = ServerSettings("")
}
