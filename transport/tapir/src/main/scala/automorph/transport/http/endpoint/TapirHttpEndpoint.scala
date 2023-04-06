package automorph.transport.http.endpoint

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.TapirHttpEndpoint.{
  Context, Request, clientAddress, getRequestContext, getRequestProperties, pathEndpointInput
}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, StringOps, ThrowableOps, TryOps}
import automorph.util.Random
import scala.collection.immutable.ListMap
import scala.util.Try
import scala.Array.emptyByteArray
import sttp.model.{Header, MediaType, Method, QueryParams, StatusCode}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{
  EndpointInput, byteArrayBody, clientIp, endpoint, header, headers, paths, queryParams, statusCode, stringToPath
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

  private lazy val contentType = Header.contentType(MediaType.parse(handler.mediaType).getOrElse {
    throw new IllegalArgumentException(s"Invalid content type: ${handler.mediaType}")
  })
  private val log = MessageLog(logger, Protocol.Http.name)
  private implicit val system: EffectSystem[Effect] = effectSystem

  def adapter: ServerEndpoint.Full[Unit, Unit, Request, Unit, (Array[Byte], StatusCode), Any, Effect] = {

    // Define server endpoint
    val publicEndpoint = pathEndpointInput(pathPrefix).map(pathInput => endpoint.in(pathInput)).getOrElse(endpoint)
    publicEndpoint.method(method).in(byteArrayBody).in(paths).in(queryParams).in(headers).in(clientIp)
      .out(byteArrayBody).out(statusCode).out(header(contentType)).serverLogic {
        case (requestBody, paths, queryParams, headers, clientIp) =>
          // Log the request
          val requestId = Random.id
          lazy val requestProperties = getRequestProperties(clientIp, Some(method), requestId)
          log.receivedRequest(requestProperties)

          // Process the request
          Try {
            val requestContext = getRequestContext(paths, queryParams, headers, Some(method))
            handler.processRequest(requestBody.toInputStream, requestContext, requestId).either.map(
              _.fold(
                error => createErrorResponse(error, clientIp, requestId, requestProperties, log),
                result => {
                  // Create the response
                  val responseBody = result.map(_.responseBody.toArray).getOrElse(emptyByteArray)
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

  override def clone(handler: RequestHandler[Effect, Context]): TapirHttpEndpoint[Effect] =
    copy(handler = handler)

  private def createErrorResponse(
    error: Throwable,
    clientIp: Option[String],
    requestId: String,
    requestProperties: => Map[String, String],
    log: MessageLog,
  ): (Array[Byte], StatusCode) = {
    log.failedProcessRequest(error, requestProperties)
    val message = error.description.asArray
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

  private val leadingSlashPattern = "^/+".r
  private val trailingSlashPattern = "/+$".r
  private val multiSlashPattern = "/+".r

  private[automorph] def pathEndpointInput(path: String): Option[EndpointInput[Unit]] = {
    val canonicalPath = multiSlashPattern.replaceAllIn(
      trailingSlashPattern.replaceAllIn(leadingSlashPattern.replaceAllIn(path, ""), ""),
      "/"
    )
    canonicalPath.split("/") match {
      case Array(head) if head.isEmpty => None
      case Array(head, tail*) =>
        Some(tail.foldLeft[EndpointInput[Unit]](stringToPath(head)) { case (current, next) =>
          current.and(stringToPath(next))
        })
      case _ => None
    }
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

  private def urlPath(paths: List[String]): String =
    paths match {
      case Nil => "/"
      case items => items.mkString("/")
    }
}
