package automorph.transport.websocket.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointMessageTransport}
import automorph.transport.http.endpoint.TapirHttpEndpoint.{clientAddress, getRequestContext, getRequestProperties}
import automorph.transport.http.{HttpContext, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, StringOps, ThrowableOps}
import automorph.util.Random
import scala.collection.immutable.ListMap
import sttp.capabilities.{Streams, WebSockets}
import sttp.model.{Header, QueryParams}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{CodecFormat, clientIp, endpoint, headers, paths, queryParams, webSocketBody}

/**
 * Tapir WebSocket endpoint message transport plugin.
 *
 * The endpoint interprets WebSocket request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as WebSocket response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://tapir.softwaremill.com Library documentation]]
 * @see
 *   [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
 */
object TapirWebSocketEndpoint extends Logging with EndpointMessageTransport {

  /** Request context type. */
  type Context = HttpContext[Unit]

  /** Endpoint request type. */
  type Request = (List[String], QueryParams, List[Header], Option[String])

  /**
   * Creates a Tapir WebSocket endpoint with the specified RPC request handler.
   *
   * The endpoint interprets WebSocket request body as a RPC request and processes it with the specified RPC handler.
   * The response returned by the RPC handler is used as WebSocket response body.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://tapir.softwaremill.com Library documentation]]
   * @see
   *   [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
   * @param handler
   *   RPC request handler
   * @tparam Effect
   *   effect type
   * @return
   *   Tapir WebSocket endpoint
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): ServerEndpoint.Full[
    Unit, Unit, Request, Unit, Array[Byte] => Effect[Array[Byte]], EffectStreams[Effect], Effect] = {
    val log = MessageLog(logger, Protocol.WebSocket.name)
    val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
    implicit val system: EffectSystem[Effect] = genericHandler.system
    val streams = new EffectStreams[Effect] {
      override type BinaryStream = Effect[Array[Byte]]
      override type Pipe[A, B] = A => Effect[B]
    }

    // Define server endpoint
    endpoint.in(paths).in(queryParams).in(headers).in(clientIp)
      .out(webSocketBody[Array[Byte], CodecFormat.OctetStream, Array[Byte], CodecFormat.OctetStream].apply(streams))
      .serverLogic { case (paths, queryParams, headers, clientIp) =>
        // Log the request
        val requestId = Random.id
        lazy val requestProperties = getRequestProperties(clientIp, None, requestId)
        logger.debug("Received WebSocket request", requestProperties)

        // Process the request
        system.successful(Right { requestBody =>
          val requestContext = getRequestContext(paths, queryParams, headers, None)
          genericHandler.processRequest(requestBody.toInputStream, requestContext, requestId).either.map(
            _.fold(
              error => createErrorResponse(error, clientIp, requestId, requestProperties, log),
              result => {
                // Create the response
                val responseBody = result.responseBody.map(_.toArray).getOrElse(Array[Byte]())
                createResponse(responseBody, clientIp, requestId, log)
              },
            )
          )
        })
      }
  }

  private def createErrorResponse(
    error: Throwable,
    clientIp: Option[String],
    requestId: String,
    requestProperties: => Map[String, String],
    log: MessageLog,
  ): Array[Byte] = {
    log.failedProcessRequest(error, requestProperties)
    val message = error.description.asArray
    createResponse(message, clientIp, requestId, log)
  }

  private def createResponse(
    responseBody: Array[Byte],
    clientIp: Option[String],
    requestId: String,
    log: MessageLog,
  ): Array[Byte] = {
    // Log the response
    lazy val responseProperties = ListMap(LogProperties.requestId -> requestId, "Client" -> clientAddress(clientIp))
    log.sendingResponse(responseProperties)
    responseBody
  }

  trait EffectStreams[Effect[_]] extends Streams[EffectStreams[Effect]] with WebSockets {

    override type BinaryStream = Effect[Array[Byte]]
    override type Pipe[A, B] = A => Effect[B]
  }
}
