package automorph.transport.http.client

import automorph.log.Logging
import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.transport.http.HttpProperties
import automorph.transport.http.client.SttpClient.Context
import java.io.IOException
import java.net.URI
import scala.collection.immutable.ArraySeq
import sttp.client3.{asByteArray, basicRequest, ignore, PartialRequest, Request, Response, SttpBackend}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP client transport plugin using HTTP as message transport protocol with the specified STTP backend.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an STTP client transport plugin with the specified STTP backend.
 * @param url endpoint URL
 * @param system effect system plugin
 * @param method HTTP method
 * @param backend STTP backend
 * @tparam Effect effect type
 */
final case class SttpClient[Effect[_]](
  url: URI,
  method: String,
  system: EffectSystem[Effect],
  backend: SttpBackend[Effect, _]
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val uri = Uri(url)
  private val httpMethod = Method.unsafeApply(method)

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = createRequest(request, mediaType, context).response(asByteArray)
    system.flatMap(
      send[Either[String, Array[Byte]]](httpRequest, request.length),
      (response: Response[Either[String, Array[Byte]]]) =>
        response.body.fold(
          error => {
            val exception = new IOException(error)
            logger.error("Failed to receive HTTP response", exception, Map("URL" -> url))
            system.failed(exception)
          },
          message => {
            logger.debug(
              "Received HTTP response",
              Map("URL" -> url, "Status" -> response.code, "Size" -> request.length)
            )
            system.pure(new ArraySeq.ofByte(message))
          }
        )
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] = {
    val httpRequest = createRequest(request, mediaType, context).response(ignore)
    system.map(send[Unit](httpRequest, request.length), (_: Response[Unit]) => ())
  }

  override def defaultContext: Context = SttpClient.defaultContext

  private def send[R](request: Request[R, Any], size: Int): Effect[Response[R]] = {
    logger.trace("Sending HTTP request", Map("URL" -> url, "Method" -> request.method, "Size" -> size))
    system.flatMap(
      system.either(request.send(backend)),
      (result: Either[Throwable, Response[R]]) =>
        result.fold(
          error => {
            logger.error(
              "Failed to send HTTP request",
              error,
              Map("URL" -> url, "Method" -> request.method, "Size" -> size)
            )
            system.failed(error)
          },
          response => {
            logger.debug("Sent HTTP request", Map("URL" -> url, "Method" -> request.method, "Size" -> size))
            system.pure(response)
          }
        )
    )
  }

  private def createRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Request[Either[String, String], Any] = {
    val contentType = MediaType.unsafeParse(mediaType)
    val properties = context.getOrElse(defaultContext)
    val requestMethod = properties.method.map(Method.unsafeApply).getOrElse(httpMethod)
    val requestUrl = properties.url.map(Uri(_)).getOrElse(uri)
    basicRequest.method(requestMethod, requestUrl)
      .contentType(contentType).header(Header.accept(contentType)).body(request.unsafeArray)
      .followRedirects(properties.followRedirects).readTimeout(properties.readTimeout)
      .headers(properties.headers.map { case (name, value) => Header(name, value) }: _*)
  }
}

case object SttpClient {

  /** Request context type. */
  type Context = HttpProperties[PartialRequest[Either[String, String], Any]]

  implicit val defaultContext: Context = HttpProperties()
}
