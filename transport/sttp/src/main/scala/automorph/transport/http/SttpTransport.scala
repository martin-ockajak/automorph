package automorph.transport.http

import automorph.spi.{Backend, Transport}
import automorph.transport.http.SttpTransport.RequestProperties
import java.io.IOException
import java.net.URL
import scala.collection.immutable.ArraySeq
import sttp.client3.{asByteArray, basicRequest, ignore, PartialRequest, Request, Response, SttpBackend}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP transport plugin using HTTP as message transport protocol with the specified STTP backend.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
 * @constructor Creates an STTP transport plugin using HTTP as message transport protocol with the specified STTP backend.
 * @param url endpoint URL
 * @param backend effect backend plugin
 * @param method HTTP method
 * @param sttpBackend STTP backend
 * @tparam Effect effect type
 */
final case class SttpTransport[Effect[_]](
  url: URL,
  method: String,
  backend: Backend[Effect],
  sttpBackend: SttpBackend[Effect, _]
) extends Transport[Effect, RequestProperties] {

  private val uri = Uri(url.toURI)
  private val httpMethod = Method.unsafeApply(method)

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = setupHttpRequest(request, mediaType, context).response(asByteArray)
    backend.flatMap(
      httpRequest.send(sttpBackend),
      (response: Response[Either[String, Array[Byte]]]) =>
        response.body.fold(
          error => backend.failed(new IOException(error)),
          response => backend.pure(new ArraySeq.ofByte(response))
        )
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[RequestProperties]): Effect[Unit] = {
    val httpRequest = setupHttpRequest(request, mediaType, context).response(ignore)
    backend.map(httpRequest.send(sttpBackend), (_: Response[Unit]) => ())
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
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param partial partially constructed request
   */
  case class RequestProperties(
    partial: PartialRequest[Either[String, String], Any] = basicRequest
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties()
  }
}
