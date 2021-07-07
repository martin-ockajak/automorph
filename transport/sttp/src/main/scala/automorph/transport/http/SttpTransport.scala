package automorph.transport.http

import automorph.spi.{Backend, Transport}
import automorph.transport.http.SttpTransport.RequestProperties
import java.io.IOException
import scala.collection.immutable.ArraySeq
import sttp.client3.{basicRequest, PartialRequest, Request, Response, SttpApi, SttpBackend}
import sttp.model.{MediaType, Header, Method, Uri}

/**
 * STTP HTTP transport using the specified STTP backend.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_2.13/latest/sttp/client3/index.html API]]
 * @param url endpoint URL
 * @param backend effect backend plugin
 * @param method HTTP method
 * @param sttpBackend STTP backend
 * @tparam Effect effect type
 */
final case class SttpTransport[Effect[_]](
  url: Uri,
  method: Method,
  backend: Backend[Effect],
  sttpBackend: SttpBackend[Effect, _]
) extends Transport[Effect, RequestProperties] with SttpApi {

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

  private def setupHttpRequest(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[RequestProperties]
  ): Request[Either[String, String], Any] = {
    val contentType = MediaType.unsafeParse(mediaType)
    val requestProperties = context.getOrElse(RequestProperties())
    requestProperties.partial.method(requestProperties.method.getOrElse(method), url)
      .contentType(contentType).header(Header.accept(contentType)).body(request.unsafeArray)
  }
}

case object SttpTransport {

  /**
   * HTTP request context.
   *
   * @see [[https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_2.13/latest/sttp/client3/RequestT.html API]]
   * @param partial partially constructed request
   */
  case class RequestProperties(
    method: Option[Method] = None,
    partial: PartialRequest[Either[String, String], Any] = basicRequest
  )

  case object RequestProperties {
    implicit val defaultContext: RequestProperties = RequestProperties()
  }
}
