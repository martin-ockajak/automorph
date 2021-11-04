package automorph.transport.websocket.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.endpoint.TapirHttpEndpoint.{clientAddress, extractRequestProperties, getRequestContext}
import automorph.util.Extensions.{EffectOps, ThrowableOps}
import automorph.util.{Bytes, Random}
import sttp.capabilities.{Streams, WebSockets}
import sttp.model.{Header, QueryParams}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{clientIp, endpoint, headers, paths, queryParams, webSocketBody, CodecFormat}

/**
 * Tapir WebSocket endpoint message transport plugin.
 *
 * The endpoint interprets WebSocket request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as WebSocket response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://tapir.softwaremill.com Library documentation]]
 * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
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
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://tapir.softwaremill.com Library documentation]]
   * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
   * @param handler RPC request handler
   * @tparam Effect effect type
   * @return Tapir WebSocket endpoint
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): ServerEndpoint[Request, Unit, Array[Byte] => Effect[Array[Byte]], EffectStreams[Effect], Effect] = {
    val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
    implicit val system: EffectSystem[Effect] = genericHandler.system
    val streams = new EffectStreams[Effect] {
      override type BinaryStream = Effect[Array[Byte]]
      override type Pipe[A, B] = A => Effect[B]
    }
    endpoint
      .in(paths).in(queryParams).in(headers).in(clientIp)
      .out(webSocketBody[Array[Byte], CodecFormat.OctetStream, Array[Byte], CodecFormat.OctetStream].apply(streams))
      .serverLogic { case (paths, queryParams, headers, clientIp) =>
        // Log the request
        val requestId = Random.id
        lazy val requestProperties = extractRequestProperties(clientIp, None, requestId)
        logger.debug("Received WebSocket request", requestProperties)

        // Process the request
        system.pure(Right { (requestBody: Array[Byte]) =>
          implicit val requestContext: Context = getRequestContext(paths, queryParams, headers, None)
          genericHandler.processRequest(requestBody, requestId, None).either.map(_.fold(
            error => createErrorResponse(error, clientIp, requestId, requestProperties),
            result => {
              // Create the response
              val responseBody = result.responseBody.getOrElse(Array[Byte]())
              createResponse(responseBody, clientIp, requestId)
            }
          ))
        })
      }
  }

  private def createErrorResponse(
    error: Throwable,
    clientIp: Option[String],
    requestId: String,
    requestProperties: => Map[String, String]
  ): Array[Byte] = {
    logger.error("Failed to process HTTP request", error, requestProperties)
    val message = Bytes.string.from(error.trace.mkString("\n")).unsafeArray
    createResponse(message, clientIp, requestId)
  }

  private def createResponse(
    responseBody: Array[Byte],
    clientIp: Option[String],
    requestId: String
  ): Array[Byte] = {
    // Log the response
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(clientIp)
    )
    logger.debug("Sending HTTP response", responseDetails)
    responseBody
  }

  trait EffectStreams[Effect[_]] extends Streams[EffectStreams[Effect]] with WebSockets {

    override type BinaryStream = Effect[Array[Byte]]
    override type Pipe[A, B] = A => Effect[B]
  }
}
