package automorph.transport.http.client

import automorph.spi.{EffectSystem, ClientMessageTransport}
import automorph.transport.http.client.SttpTransport.RequestProperties
import java.io.IOException
import java.net.URL
import scala.collection.immutable.ArraySeq
import sttp.client3.{PartialRequest, Request, Response, SttpBackend, asByteArray, basicRequest, ignore}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP transport plugin using HTTP as message transport protocol with the specified STTP backend.
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
final case class SttpTransport[Effect[_]](
  url: URL,
  method: String,
  system: EffectSystem[Effect],
  backend: SttpBackend[Effect, _]
) extends ClientMessageTransport[Effect, RequestProperties] {

  private val uri = Uri(url.toURI)
  private val httpMethod = Method.unsafeApply(method)

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = setupHttpRequest(request, mediaType, context).response(asByteArray)
    system.flatMap(
      httpRequest.send(backend),
      (response: Response[Either[String, Array[Byte]]]) =>
        response.body.fold(
          error => system.failed(new IOException(error)),
          response => system.pure(new ArraySeq.ofByte(response))
        )
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Effect[Unit] = {
    val httpRequest = setupHttpRequest(request, mediaType, context).response(ignore)
    system.map(httpRequest.send(backend), (_: Response[Unit]) => ())
  }

  override def defaultContext: RequestProperties = RequestProperties.defaultContext

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

case object SttpTransport {

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
