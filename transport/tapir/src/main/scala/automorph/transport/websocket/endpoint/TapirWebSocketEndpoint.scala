package automorph.transport.websocket.endpoint

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.endpoint.TapirHttpEndpoint.{extractRequestProperties, requestContext}
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Random}
import sttp.capabilities.{Streams, WebSockets}
import sttp.model.{Header, MediaType, Method, QueryParams, StatusCode}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{CodecFormat, byteArrayBody, clientIp, endpoint, header, headers, paths, queryParams, statusCode, webSocketBody}

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
  type Context = Http[Unit]

  /** Endpoint request type. */
  type RequestType = (List[String], QueryParams, List[Header], Option[String])

// FIXME - finish WebSocket support
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
  def apply[Effect[_], S](
    handler: Types.HandlerAnyCodec[Effect, Context],
    streams: Streams[S with WebSockets]
  ): ServerEndpoint[RequestType, Unit, streams.Pipe[Array[Byte], Array[Byte]], S with WebSockets, Effect] = {
    val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
    val system = genericHandler.system
    val contentType = Header.contentType(MediaType.parse(genericHandler.protocol.codec.mediaType).getOrElse {
      throw new IllegalArgumentException(s"Invalid content type: ${genericHandler.protocol.codec.mediaType}")
    })
    endpoint
      .in(paths).in(queryParams).in(headers).in(clientIp)
      .out(webSocketBody[Array[Byte], CodecFormat.OctetStream, Array[Byte], CodecFormat.OctetStream].apply(streams))
      .serverLogic { case (paths, queryParams, headers, clientIp) =>
        // Log the request
        val requestId = Random.id
        lazy val requestProperties = extractRequestProperties(clientIp, None, requestId)
        logger.debug("Received WebSocket request", requestProperties)

        // Process the request
        implicit val usingContext: Context = requestContext(paths, queryParams, headers, None)
        ???
//        ""
//        system.map(
//          system.either(genericHandler.processRequest(requestMessage, requestId, None)),
//          (handlerResult: Either[Throwable, HandlerResult[Array[Byte], Context]]) =>
//            handlerResult.fold(
//              error => Right(serverError(error, clientIp, requestId, requestProperties)),
//              result => {
//                // Send the response
//                val message = result.responseBody.getOrElse(Array[Byte]())
//                Right(createResponse(message, status, clientIp, requestId))
//              }
//            )
//        )
      }
  }
}

trait EffectStreams[Effect[_]] extends Streams[EffectStreams[Effect]] {
  override type BinaryStream = Effect[Array[Byte]]
  override type Pipe[A, B] = Effect[A] => Effect[B]
}
