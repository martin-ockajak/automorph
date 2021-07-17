package automorph.transport.http.client

import automorph.log.Logging
import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.transport.http.client.SttpClient.RequestProperties
import java.io.IOException
import java.net.URL
import scala.collection.immutable.ArraySeq
import sttp.client3.{PartialRequest, Request, Response, SttpBackend, asByteArray, basicRequest, ignore}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP client transport plugin using HTTP as message transport protocol with the specified STTP backend.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an STTP transport plugin using HTTP as message transport protocol with the specified STTP system.
 * @param url endpoint URL
 * @param system effect system plugin
 * @param method HTTP method
 * @param backend STTP backend
 * @tparam Effect effect type
 */
final case class SttpClient[Effect[_]](
  url: URL,
  method: String,
  system: EffectSystem[Effect],
  backend: SttpBackend[Effect, _]
) extends ClientMessageTransport[Effect, RequestProperties] with Logging {

  private val uri = Uri(url.toURI)
  private val httpMethod = Method.unsafeApply(method)
  private val urlText = url.toExternalForm

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = setupHttpRequest(request, mediaType, context).response(asByteArray)
    system.flatMap(
      send[Either[String, Array[Byte]]](httpRequest, request.length),
      (response: Response[Either[String, Array[Byte]]]) =>
        response.body.fold(
          error => {
            val exception = new IOException(error)
            logger.error("Failed to receive HTTP response", exception, Map("URL" -> urlText))
            system.failed(exception)
          },
          message => {
            logger.debug("Received HTTP response", Map("URL" -> urlText, "Status" -> response.code, "Size" -> request.length))
            system.pure(new ArraySeq.ofByte(message))
          }
        )
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Effect[Unit] = {
    val httpRequest = setupHttpRequest(request, mediaType, context).response(ignore)
    system.map(send[Unit](httpRequest, request.length), (_: Response[Unit]) => ())
  }

  override def defaultContext: RequestProperties = RequestProperties.defaultContext

  private def send[R](request: Request[R, Any], size: Int): Effect[Response[R]] = {
    logger.trace("Sending HTTP request", Map("URL" -> urlText, "Method" -> request.method, "Size" -> size))
    system.map(request.send(backend), (response: Response[R]) => {
      logger.debug("Sent HTTP request", Map("URL" -> urlText, "Method" -> request.method, "Size" -> size))
      response
    })
  }

  private def setupHttpRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Request[Either[String, String], Any] = {
    val contentType = MediaType.unsafeParse(mediaType)
    val requestProperties = context.getOrElse(defaultContext)
    requestProperties.partial.method(requestProperties.partial.method.getOrElse(httpMethod), uri)
      .contentType(contentType).header(Header.accept(contentType)).body(request.unsafeArray)
  }
}

case object SttpClient {

  /** Request context type. */
  type Context = RequestProperties

  /**
   * HTTP request context.
   *
   * @see [[https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_2.13/latest/sttp/client3/RequestT.html API]]
   * @param partial partially constructed request
   */
  case class RequestProperties(
    partial: PartialRequest[Either[String, String], Any] = basicRequest
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties()
  }
}
