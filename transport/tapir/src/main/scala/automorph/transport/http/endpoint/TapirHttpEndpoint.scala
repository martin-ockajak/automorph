package automorph.transport.http.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, StringOps, ThrowableOps}
import automorph.util.Random
import scala.collection.immutable.ListMap
import sttp.model.{Header, MediaType, Method, QueryParams, StatusCode}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{byteArrayBody, clientIp, endpoint, header, headers, paths, queryParams, statusCode}

/**
 * Tapir HTTP endpoint message transport plugin.
 *
 * The endpoint interprets HTTP request body as an RPC request and processes it using the specified RPC handler. The
 * response returned by the RPC handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://tapir.softwaremill.com Library documentation]]
 * @see
 *   [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
 */
object TapirHttpEndpoint extends Logging with EndpointMessageTransport {

  /** Request context type. */
  type Context = HttpContext[Unit]

  /** Endpoint request type. */
  type Request = (Array[Byte], List[String], QueryParams, List[Header], Option[String])

  /**
   * Creates a Tapir HTTP endpoint with the specified RPC request handler.
   *
   * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler. The
   * response returned by the RPC handler is used as HTTP response body.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://tapir.softwaremill.com Library documentation]]
   * @see
   *   [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
   * @param handler
   *   RPC request handler
   * @param method
   *   HTTP method to server
   * @param mapException
   *   maps an exception to a corresponding HTTP status code
   * @tparam Effect
   *   effect type
   * @return
   *   Tapir HTTP endpoint
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    method: Method,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  ): ServerEndpoint[Request, Unit, (Array[Byte], StatusCode), Any, Effect] = {
    val log = MessageLog(logger, Protocol.Http.name)
    val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
    val system = genericHandler.system
    implicit val givenSystem: EffectSystem[Effect] = system
    val contentType = Header.contentType(MediaType.parse(genericHandler.protocol.codec.mediaType).getOrElse {
      throw new IllegalArgumentException(s"Invalid content type: ${genericHandler.protocol.codec.mediaType}")
    })

    // Define server endpoint
    endpoint.method(method).in(byteArrayBody).in(paths).in(queryParams).in(headers).in(clientIp).out(byteArrayBody)
      .out(statusCode).out(header(contentType)).serverLogic {
        case (requestBody, paths, queryParams, headers, clientIp) =>
          // Log the request
          val requestId = Random.id
          lazy val requestProperties = getRequestProperties(clientIp, Some(method), requestId)
          log.receivedRequest(requestProperties)

          // Process the request
          val requestContext = getRequestContext(paths, queryParams, headers, Some(method))
          genericHandler.processRequest(requestBody.toInputStream, requestContext, requestId).either.map(
            _.fold(
              error => Right(createErrorResponse(error, clientIp, requestId, requestProperties, log)),
              result => {
                // Create the response
                val responseBody = result.responseBody.map(_.toArray).getOrElse(Array[Byte]())
                val status = result.exception.map(mapException).map(StatusCode.apply).getOrElse(StatusCode.Ok)
                Right(createResponse(responseBody, status, clientIp, requestId, log))
              },
            )
          )
      }
  }

  private[automorph] def getRequestContext(
    paths: List[String],
    queryParams: QueryParams,
    headers: List[Header],
    method: Option[Method],
  ): Context =
    HttpContext(
      transport = Some(()),
      method = method.map(_.toString).map(HttpMethod.valueOf),
      path = Some(urlPath(paths)),
      parameters = queryParams.toSeq,
      headers = headers.map(header => header.name -> header.value),
    )

  private[automorph] def urlPath(paths: List[String]): String =
    paths match {
      case Nil => "/"
      case items => items.mkString("/")
    }

  private[automorph] def getRequestProperties(
    clientIp: Option[String],
    method: Option[Method],
    requestId: String,
  ): Map[String, String] =
    ListMap(LogProperties.requestId -> requestId, "Client" -> clientAddress(clientIp)) ++
      method.map("Method" -> _.toString)

  private[automorph] def clientAddress(clientIp: Option[String]): String =
    clientIp.getOrElse("")

  private def createErrorResponse(
    error: Throwable,
    clientIp: Option[String],
    requestId: String,
    requestProperties: => Map[String, String],
    log: MessageLog,
  ): (Array[Byte], StatusCode) = {
    log.failedProcessRequest(error, requestProperties)
    val message = error.description.toArray
    val status = StatusCode.InternalServerError
    createResponse(message, status, clientIp, requestId, log)
  }

  private def createResponse(
    responseBody: Array[Byte],
    status: StatusCode,
    clientIp: Option[String],
    requestId: String,
    log: MessageLog,
  ): (Array[Byte], StatusCode) = {
    // Log the response
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(clientIp),
      "Status" -> statusCode.toString,
    )
    log.sendingResponse(responseProperties)
    (responseBody, status)
  }
}
