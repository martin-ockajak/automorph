package automorph.transport.http.endpoint

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.TapirHttpEndpoint.{
  Context, MessageFormat, Request, clientAddress, getRequestContext, getRequestProperties, pathComponents,
  pathEndpointInput
}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{EffectOps, StringOps, ThrowableOps, TryOps}
import automorph.util.Random
import scala.collection.immutable.ListMap
import scala.util.Try
import sttp.model.{Header, MediaType, Method, QueryParams, StatusCode}
import sttp.tapir.Codec.id
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{
  CodecFormat, EndpointIO, EndpointInput, RawBodyType, Schema, clientIp, endpoint, headers, paths, queryParams,
  statusCode, stringToPath
}

/**
 * Tapir HTTP endpoint message transport plugin.
 *
 * Interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 *   - The response returned by the RPC request handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/HTTP Transport protocol]]
 * @see
 *   [[https://tapir.softwaremill.com Library documentation]]
 * @see
 *   [[https://javadoc.io/doc/com.softwaremill.sttp.tapir/tapir-core_3/latest/index.html API]]
 * @constructor
 *   Creates a Tapir HTTP endpoint message transport plugin with specified effect system and request handler.
 * @param effectSystem
 *   effect system plugin
 * @param pathPrefix
 *   HTTP URL path prefix, only requests starting with this path prefix are allowed
 * @param method
 *   HTTP method
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param handler
 *   RPC request handler
 */
final case class TapirHttpEndpoint[Effect[_]](
  effectSystem: EffectSystem[Effect],
  pathPrefix: String = "/",
  method: Method = Method.POST,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends Logging with EndpointTransport[
  Effect,
  Context,
  ServerEndpoint.Full[Unit, Unit, Request, Unit, (Array[Byte], StatusCode), Any, Effect]
] {

  private val prefixPaths = pathComponents(pathPrefix)
  private lazy val mediaType = MediaType.parse(handler.mediaType).fold(
    error => throw new IllegalStateException(
      s"Invalid message content type: ${handler.mediaType}", new IllegalArgumentException(error)
    ),
    identity,
  )
  private lazy val codec = id[Array[Byte], MessageFormat](MessageFormat(mediaType), Schema.schemaForByteArray)
  private lazy val body = EndpointIO.Body(RawBodyType.ByteArrayBody, codec, EndpointIO.Info.empty)
  private val log = MessageLog(logger, Protocol.Http.name)
  private implicit val system: EffectSystem[Effect] = effectSystem

  def adapter: ServerEndpoint.Full[Unit, Unit, Request, Unit, (Array[Byte], StatusCode), Any, Effect] = {
    // Define server endpoint inputs & outputs
    val endpointPath = pathEndpointInput(prefixPaths).map(pathInput => endpoint.in(pathInput)).getOrElse(endpoint)
    val endpointInput = endpointPath.method(method).in(body).in(paths).in(queryParams).in(headers).in(clientIp)
    val endpointOutput = endpointInput.out(body).out(statusCode)
    endpointOutput.serverLogic {
      case (requestBody, paths, queryParams, headers, clientIp) =>
        // Log the request
        val requestId = Random.id
        lazy val requestProperties = getRequestProperties(clientIp, Some(method), requestId)
        log.receivedRequest(requestProperties)

        // Process the request
        Try {
          val requestContext = getRequestContext(prefixPaths ++ paths, queryParams, headers, Some(method))
          val handlerResult = handler.processRequest(requestBody, requestContext, requestId)
          handlerResult.either.map(
            _.fold(
              error => createErrorResponse(error, clientIp, requestId, requestProperties, log),
              result => {
                // Create the response
                val responseBody = result.map(_.responseBody).getOrElse(Array.emptyByteArray)
                val status = result.flatMap(_.exception).map(mapException).map(StatusCode.apply)
                  .getOrElse(StatusCode.Ok)
                createResponse(responseBody, status, clientIp, requestId, log)
              },
            )
          )
        }.foldError { error =>
          effectSystem.evaluate(
            createErrorResponse(error, clientIp, requestId, requestProperties, log)
          )
        }.map(Right.apply)
    }
  }

  override def withHandler(handler: RequestHandler[Effect, Context]): TapirHttpEndpoint[Effect] =
    copy(handler = handler)

  private def createErrorResponse(
    error: Throwable,
    clientIp: Option[String],
    requestId: String,
    requestProperties: => Map[String, String],
    log: MessageLog,
  ): (Array[Byte], StatusCode) = {
    log.failedProcessRequest(error, requestProperties)
    val message = error.description.toByteArray
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

case object TapirHttpEndpoint {

  /** Request context type. */
  type Context = HttpContext[Unit]

  /** Endpoint request type. */
  type Request = (Array[Byte], List[String], QueryParams, List[Header], Option[String])

  private[automorph] final case class MessageFormat(mediaType: MediaType) extends CodecFormat

  private val leadingSlashPattern = "^/+".r
  private val trailingSlashPattern = "/+$".r
  private val multiSlashPattern = "/+".r

  private[automorph] def pathComponents(path: String): List[String] = {
    val canonicalPath = multiSlashPattern.replaceAllIn(
      trailingSlashPattern.replaceAllIn(leadingSlashPattern.replaceAllIn(path, ""), ""),
      "/"
    )
    canonicalPath.split("/") match {
      case Array(head) if head.isEmpty => List.empty
      case components => components.toList
    }
  }

  private[automorph] def pathEndpointInput(pathComponents: Seq[String]): Option[EndpointInput[Unit]] =
    pathComponents match {
      case Seq() => None
      case Seq(head, tail*) =>
        Some(tail.foldLeft[EndpointInput[Unit]](stringToPath(head)) { case (current, next) =>
          current.and(stringToPath(next))
        })
    }

  private[automorph] def getRequestContext(
    paths: List[String],
    queryParams: QueryParams,
    headers: List[Header],
    method: Option[Method],
  ): Context =
    HttpContext(
      transportContext = Some {},
      method = method.map(_.toString).map(HttpMethod.valueOf),
      path = Some(urlPath(paths)),
      parameters = queryParams.toSeq,
      headers = headers.map(header => header.name -> header.value),
    )

  private[automorph] def getRequestProperties(
    clientIp: Option[String],
    method: Option[Method],
    requestId: String,
  ): Map[String, String] =
    ListMap(LogProperties.requestId -> requestId, "Client" -> clientAddress(clientIp)) ++
      method.map("Method" -> _.toString)

  private[automorph] def clientAddress(clientIp: Option[String]): String =
    clientIp.getOrElse("")

  private def urlPath(paths: List[String]): String = {
    s"/${paths.mkString("/")}"
  }
}
