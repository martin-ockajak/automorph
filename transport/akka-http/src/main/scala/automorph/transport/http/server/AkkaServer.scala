package automorph.transport.http.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.{MethodNotAllowed, NotFound}
import akka.http.scaladsl.server.Directives.{complete, extractRequest}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import automorph.log.Logging
import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
import automorph.transport.http.endpoint.AkkaHttpEndpoint
import automorph.transport.http.server.AkkaServer.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.Await

/**
 * Akka HTTP server transport plugin.
 *
 * Interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 *   - The response returned by the RPC request handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://doc.akka.io/docs/akka-http Library documentation]]
 * @see
 *   [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
 * @constructor
 *   Creates and starts an Akka HTTP server with specified RPC request handler.
 * @param effectSystem
 *   effect system plugin
 * @param port
 *   port to listen on for HTTP connections
 * @param pathPrefix
 *   HTTP URL path prefix, only requests starting with this path prefix are allowed
 * @param methods
 *   allowed HTTP request methods
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param readTimeout
 *   HTTP request read timeout
 * @param requestTimeout
 *   HTTP request processing timeout
 * @param handler
 *   RPC request handler
 * @param serverSettings
 *   HTTP server settings
 * @tparam Effect
 *   effect type
 */
final case class AkkaServer[Effect[_]](
  effectSystem: EffectSystem[Effect],
  port: Int,
  pathPrefix: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  readTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS),
  requestTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS),
  serverSettings: ServerSettings = ServerSettings(""),
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends Logging with ServerTransport[Effect, Context] {

  implicit private val system: EffectSystem[Effect] = effectSystem
  private val allowedMethods = methods.map(_.name).toSet
  private var actorSystem: Option[ActorSystem[Nothing]] = Option.empty[ActorSystem[Nothing]]

  override def clone(handler: RequestHandler[Effect, Context]): AkkaServer[Effect] =
    copy(handler = handler)

  override def init(): Effect[Unit] =
    system.evaluate(this.synchronized {
      val behavior = Behaviors.setup[Nothing] { actorContext =>
        // Create handler actor
        implicit val actorSystem: ActorSystem[Nothing] = actorContext.system
        val endpointTransport = AkkaHttpEndpoint(effectSystem, mapException, readTimeout, handler)

        // Create HTTP route
        val endpointRoute = endpointTransport.route(actorContext, requestTimeout)
        val serverRoute = route(endpointRoute)

        // Start HTTP server
        val serverBinding = Await.result(
          Http().newServerAt("0.0.0.0", port).withSettings(serverSettings).bind(serverRoute),
          Duration.Inf,
        )
        logger.info(
          "Listening for connections",
          ListMap(
            "Protocol" -> Protocol.Http,
            "Port" -> serverBinding.localAddress.getPort.toString
          ),
        )
        Behaviors.empty
      }
      actorSystem = Some(ActorSystem[Nothing](behavior, getClass.getSimpleName))
      ()
    })

  override def close(): Effect[Unit] =
    effectSystem.evaluate(this.synchronized {
      actorSystem.fold(
        throw new IllegalStateException(s"${getClass.getSimpleName} already closed")
      ) { activeActorSystem =>
        activeActorSystem.terminate()
        Await.result(activeActorSystem.whenTerminated, Duration.Inf)
        actorSystem = None
      }
    })

  private def route(endpointRoute: Route): Route =
    // Validate HTTP request method
    extractRequest { httpRequest =>
      if (allowedMethods.contains(httpRequest.method.value.toUpperCase)) {
        // Validate URL path
        if (httpRequest.uri.path.toString.startsWith(pathPrefix)) {
          endpointRoute
        } else {
          complete(NotFound)
        }
      } else {
        complete(MethodNotAllowed)
      }
    }
}

object AkkaServer {

  /** Request context type. */
  type Context = AkkaHttpEndpoint.Context
}
