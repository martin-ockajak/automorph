package automorph.transport.http.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.MethodNotAllowed
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, extractClientIP, extractMethod, extractRequest, rawPathPrefixTest}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import automorph.Types
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.endpoint.AkkaHttpEndpoint
import automorph.transport.http.endpoint.AkkaHttpEndpoint.Message
import automorph.transport.http.server.AkkaServer.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

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
 * @param serverSettings HTTP server settings
 * @param terminationTimeout timeout after which all requests and connections shall be forcefully terminated
 * @param actorSystem Akka actor system
 * @tparam Effect effect type
 */
final case class AkkaServer[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  port: Int,
  pathPrefix: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  serverSettings: ServerSettings = AkkaServer.defaultServerSettings,
  terminationTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS),
)(implicit actorSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, getClass.getName))
  extends Logging with ServerMessageTransport[Effect] {

  private val messageMethodNotAllowed = "Method Not Allowed"
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system
  private val allowedMethods = methods.map(_.name).toSet
  private lazy val route = createRoute()
  private val server = start()

  override def close(): Effect[Unit] = {
    system.wrap {
      Await.result(server.terminate(terminationTimeout), Duration.Inf)
      ()
    }
  }

  private def createRoute(): Route = {
    implicit val executionContext: ExecutionContext = actorSystem.executionContext

    // Validate HTTP request method
    extractMethod { httpMethod =>
      // Validate URL path
      rawPathPrefixTest(pathPrefix) {
        // Process request
        extractRequest { httpRequest =>
          if (allowedMethods.contains(httpMethod.value.toUpperCase)) {
            extractClientIP { remoteAddress =>
              val message = Message(httpRequest, null, remoteAddress)
              complete(HttpEntity(ContentTypes.`application/json`, "{}"))
            }
          } else {
            complete(MethodNotAllowed, messageMethodNotAllowed)
          }
        }
      }
    }
  }

  private def start(): Http.ServerBinding = {
    val serverBinding = Await.result(
      Http().newServerAt("0.0.0.0", port).withSettings(serverSettings).bind(route),
      Duration.Inf
    )
    val properties = ListMap(
      "Protocol" -> Protocol.Http,
      "Port" -> serverBinding.localAddress.getPort.toString
    )
    logger.info("Listening for connections", properties)
    serverBinding
  }
}

object AkkaServer {

  /** Request context type. */
  type Context = AkkaHttpEndpoint.Context

  /** Default HTTP server settings. */
  def defaultServerSettings: ServerSettings = ServerSettings("")
}
