package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.MessageCodec
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.util.{Bytes, Random}
import automorph.util.Extensions.ThrowableOps
import sttp.capabilities.{Streams, WebSockets}
import sttp.model.headers.Cookie
import sttp.model.{Header, MediaType, Method, QueryParams, StatusCode}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{CodecFormat, byteArrayBody, clientIp, cookies, endpoint, header, headers, paths, queryParams, statusCode, webSocketBody}

/**
 * Tapir HTTP endpoint message transport plugin.
 *
 * The endpoint interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://tapir.softwaremill.com Library documentation]]
 * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
 */
object TapirHttpEndpoint extends Logging with EndpointMessageTransport {

  /** Request context type. */
  type Context = Http[Unit]

  /** Endpoint request type. */
  type RequestType = (Array[Byte], List[String], QueryParams, List[Header], List[Cookie], Option[String])

  /** Endpoint request type. */
  type XRequestType = (List[String], QueryParams, List[Header], List[Cookie], Option[String])

  /**
   * Creates a Tapir HTTP endpoint with the specified RPC request handler.
   *
   * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
   * The response returned by the RPC handler is used as HTTP response body.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://tapir.softwaremill.com Library documentation]]
   * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
   * @param handler RPC request handler
   * @param method HTTP method to server
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @tparam Effect effect type
   * @return Tapir HTTP endpoint
   */
  def apply[Effect[_]](
    handler: Handler.AnyCodec[Effect, Context],
    method: Method,
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
  ): ServerEndpoint[RequestType, Unit, (Array[Byte], StatusCode), Any, Effect] = {
    val system = handler.system
    val contentType = Header.contentType(MediaType.parse(handler.protocol.codec.mediaType).getOrElse {
      throw new IllegalArgumentException(s"Invalid content type: ${handler.protocol.codec.mediaType}")
    })
    endpoint.method(method).in(byteArrayBody).in(paths).in(queryParams).in(headers).in(cookies).in(clientIp)
      .out(byteArrayBody).out(header(contentType)).out(statusCode)
      .serverLogic { case (requestMessage, paths, queryParams, headers, cookies, clientIp) =>
        // Receive the request
        val requestId = Random.id
        lazy val requestDetails = requestProperties(clientIp, method, requestId)
        logger.debug("Received HTTP request", requestDetails)

        // Process the request
        implicit val usingContext: Context = createContext(method, paths, queryParams, headers)
        system.map(
          system.either(handler.processRequest(requestMessage, requestId)),
          (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
            handlerResult.fold(
              error => Right(serverError(error, requestMessage, clientIp, requestId, requestDetails)),
              result => {
                // Send the response
                val message = result.response.getOrElse(Array[Byte]())
                val status = result.exception.map(exceptionToStatusCode).map(StatusCode.apply).getOrElse(StatusCode.Ok)
                Right(createResponse(message, status, clientIp, requestId))
              }
            )
        )
      }
  }

// FIXME - finish WebSocket support
//  /**
//   * Creates a Tapir HTTP endpoint with the specified RPC request handler.
//   *
//   * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
//   * The response returned by the RPC handler is used as HTTP response body.
//   *
//   * @see [[https://tapir.softwaremill.com/ Documentation]]
//   * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
//   * @param handler RPC request handler
//   * @tparam Effect effect type
//   * @return Tapir HTTP endpoint
//   */
//  def apply[Effect[_], S](
//    handler: Handler.AnyCodec[Effect, Context],
//    streams: Streams[S]
//  ): ServerEndpoint[XRequestType, Unit, streams.Pipe[Array[Byte], Array[Byte]], WebSockets, Effect] = {
//    val system = handler.system
//    val contentType = Header.contentType(MediaType.parse(handler.codec.mediaType).getOrElse {
//      throw new IllegalArgumentException(s"Invalid content type: ${handler.codec.mediaType}")
//    })
//    endpoint
//      .in(paths).in(queryParams).in(headers).in(cookies).in(clientIp)
//      .out(webSocketBody[Array[Byte], CodecFormat.OctetStream, Array[Byte], CodecFormat.OctetStream].apply[S](streams))
//      .serverLogic { case (requestMessage, paths, queryParams, headers, cookies, clientIp) =>
//        ???
////        // Receive the request
////        val requestId = Random.id
////        lazy val requestDetails = requestProperties(clientIp, method, requestId)
////        logger.debug("Received WebSocket request", Map("Client" -> client, "Size" -> requestMessage.length))
////
////        // Process the request
////        implicit val usingContext: Context = createContext(method, paths, queryParams, headers)
////        system.map(
////          system.either(handler.processRequest(requestMessage)),
////          (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
////            handlerResult.fold(
////              error => Right(serverError(error, requestMessage, clientIp, requestId, requestDetails)),
////              result => {
////                // Send the response
////                val message = result.response.getOrElse(Array[Byte]())
////                val status = result.exception.map(exceptionToStatusCode).map(StatusCode.apply).getOrElse(StatusCode.Ok)
////                Right(createResponse(message, status, clientIp, requestId))
////              }
////            )
////        )
//      }
//  }

  private def serverError(
    error: Throwable,
    request: Array[Byte],
    clientIp: Option[String],
    requestId: String,
    requestDetails: => Map[String, String]
  ): (Array[Byte], StatusCode) = {
    logger.error("Failed to process HTTP request", error, requestDetails)
    val message = Bytes.string.from(error.trace.mkString("\n")).unsafeArray
    val status = StatusCode.InternalServerError
    createResponse(message, status, clientIp, requestId)
  }

  private def createResponse(
    message: Array[Byte],
    status: StatusCode,
    clientIp: Option[String],
    requestId: String
  ): (Array[Byte], StatusCode) = {
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(clientIp),
      "Status" -> statusCode.toString
    )
    logger.debug("Sending HTTP response", responseDetails)
    (message, status)
  }

  private def createContext(
    method: Method,
    paths: List[String],
    queryParams: QueryParams,
    headers: List[Header]
  ): Context =
    Http(
      base = Some(()),
      method = Some(method.method),
      path = Some(paths.mkString("/")),
      parameters = queryParams.toSeq,
      headers = headers.map(header => header.name -> header.value).toSeq
    )

  private def requestProperties(
    clientIp: Option[String],
    method: Method,
    requestId: String
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    "Client" -> clientAddress(clientIp),
    "Method" -> Method.toString
  )

  private def clientAddress(clientIp: Option[String]): String = clientIp.getOrElse("[unknown]")
}
