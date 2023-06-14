package automorph.transport.websocket.endpoint

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.TapirHttpEndpoint.{
  clientAddress, getRequestContext, getRequestProperties, pathComponents, pathEndpointInput
}
import automorph.transport.http.{HttpContext, Protocol}
import automorph.transport.websocket.endpoint.TapirWebSocketEndpoint.{Context, EffectStreams, Request}
import automorph.util.Extensions.{EffectOps, StringOps, ThrowableOps}
import automorph.util.Random
import scala.collection.immutable.ListMap
import sttp.capabilities.{Streams, WebSockets}
import sttp.model.{Header, QueryParams}
import sttp.tapir.CodecFormat.OctetStream
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{clientIp, endpoint, headers, paths, queryParams, webSocketBody}

/**
 * Tapir WebSocket endpoint message transport plugin.
 *
 * Interprets WebSocket request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as WebSocket response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/HTTP Transport protocol]]
 * @see
 *   [[https://tapir.softwaremill.com Library documentation]]
 * @see
 *   [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
 * @constructor
 *   Creates a Tapir WebSocket endpoint with the specified RPC request handler.
 * @param effectSystem
 *   effect system plugin
 * @param pathPrefix
 *   HTTP URL path prefix, only requests starting with this path prefix are allowed
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param handler
 *   RPC request handler
 * @tparam Effect
 *   effect type
 * @tparam TapirStreams
 *   Tapir streams type
 */
final case class TapirWebSocketEndpoint[Effect[_], TapirStreams <: Streams[TapirStreams]](
  effectSystem: EffectSystem[Effect],
  streams: TapirStreams,
  pathPrefix: String = "/",
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends Logging with EndpointTransport[
  Effect,
  Context,
  ServerEndpoint.Full[Unit, Unit, Request, Unit, Array[Byte] => Effect[Array[Byte]], TapirStreams & WebSockets, Effect]
] {

  private val prefixPaths = pathComponents(pathPrefix)
  private val log = MessageLog(logger, Protocol.WebSocket.name)
  private implicit val system: EffectSystem[Effect] = effectSystem

  override def adapter: ServerEndpoint.Full[
    Unit, Unit, Request, Unit, Array[Byte] => Effect[Array[Byte]], TapirStreams & WebSockets, Effect
  ] = {

    // Define server endpoint inputs & outputs
    val xstreams = new EffectStreams[Effect] {
      override type BinaryStream = Effect[Array[Byte]]
      override type Pipe[A, B] = A => Effect[B]
    }
    val endpointPath = pathEndpointInput(prefixPaths).map(pathInput => endpoint.in(pathInput)).getOrElse(endpoint)
    val endpointInput = endpointPath.in(paths).in(queryParams).in(headers).in(clientIp)
    val webSocketBodyBuilder = webSocketBody[Array[Byte], OctetStream, Array[Byte], OctetStream]
    val endpointOutput = endpointInput.out(webSocketBodyBuilder(xstreams))

    // def serverLogic[F[_]](
    //   f: I => F[Either[E, O]]
    // )(implicit aIsUnit: A =:= Unit): ServerEndpoint.Full[Unit, Unit, I, E, O, R, F]

    val logic = endpointOutput.serverLogic { case (paths, queryParams, headers, clientIp) =>
      // Log the request
      val requestId = Random.id
      lazy val requestProperties = getRequestProperties(clientIp, None, requestId)
      log.receivedRequest(requestProperties)

      system.successful(Right { requestBody =>
        val requestContext = getRequestContext(prefixPaths ++ paths, queryParams, headers, None)
        val handlerResult = handler.processRequest(requestBody, requestContext, requestId)
        handlerResult.either.map(
          _.fold(
            error => createErrorResponse(error, clientIp, requestId, requestProperties, log),
            result => {
              // Create the response
              val responseBody = result.map(_.responseBody).getOrElse(Array.emptyByteArray)
              createResponse(responseBody, clientIp, requestId, log)
            },
          )
        )
      })
    }
    logic.asInstanceOf[ServerEndpoint.Full[
      Unit,
      Unit,
      (List[String], QueryParams, List[Header], Option[String]),
      Unit,
      Array[Byte] => Effect[Array[Byte]],
      TapirStreams & WebSockets,
      Effect
    ]]
  }

  override def withHandler(handler: RequestHandler[Effect, Context]): TapirWebSocketEndpoint[Effect, TapirStreams] =
    copy(handler = handler)

  private def createErrorResponse(
    error: Throwable,
    clientIp: Option[String],
    requestId: String,
    requestProperties: => Map[String, String],
    log: MessageLog,
  ): Array[Byte] = {
    log.failedProcessRequest(error, requestProperties)
    val message = error.description.toByteArray
    createResponse(message, clientIp, requestId, log)
  }

  private def createResponse(
    responseBody: Array[Byte],
    clientIp: Option[String],
    requestId: String,
    log: MessageLog,
  ): Array[Byte] = {
    // Log the response
    lazy val responseProperties =
      ListMap(LogProperties.requestId -> requestId, LogProperties.client -> clientAddress(clientIp))
    log.sendingResponse(responseProperties)
    responseBody
  }
}

case object TapirWebSocketEndpoint {

  /** Request context type. */
  type Context = HttpContext[Unit]

  /** Endpoint request type. */
  type Request = (List[String], QueryParams, List[Header], Option[String])

  trait EffectStreams[Effect[_]] extends Streams[EffectStreams[Effect]] with WebSockets {

    override type BinaryStream = Effect[Array[Byte]]
    override type Pipe[A, B] = A => Effect[B]
  }
}