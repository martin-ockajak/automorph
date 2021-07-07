package automorph.transport.http

import automorph.spi.{Backend, Transport}
import automorph.transport.http.SttpTransport.Request
import java.io.IOException
import scala.collection.immutable.ArraySeq
import sttp.client3.{basicRequest, Identity, PartialRequest, Request, Response, SttpApi, SttpBackend}
import sttp.model.{Header, Method, Uri}

/**
 * STTP HTTP transport using the specified STTP backend.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_2.13/latest/sttp/client3/index.html API]]
 * @param backend effect backend plugin
 * @param url endpoint URL
 * @param method HTTP method
 * @param contentType HTTP request Content-Type
 * @param sttpBackend STTP backend
 * @tparam Effect effect type
 */
final case class SttpTransport[Effect[_]](
  backend: Backend[Effect],
  url: Uri,
  method: Method,
  contentType: String,
  sttpBackend: SttpBackend[Effect, _]
) extends Transport[Effect, Request] with SttpApi {

  override def call(request: ArraySeq.ofByte, context: Option[Request]): Effect[ArraySeq.ofByte] = {
    val httpRequest = setupHttpRequest(request, context).response(asByteArray)
    backend.flatMap(
      httpRequest.send(sttpBackend),
      (response: Response[Either[String, Array[Byte]]]) =>
        response.body.fold(
          error => backend.failed(new IOException(error)),
          response => backend.pure(new ArraySeq.ofByte(response))
        )
    )
  }

  override def notify(request: ArraySeq.ofByte, context: Option[Request]): Effect[Unit] = {
    val httpRequest = setupHttpRequest(request, context).response(ignore)
    backend.map(httpRequest.send(sttpBackend), (_: Response[Unit]) => ())
  }

  private def setupHttpRequest(
    request: ArraySeq.ofByte,
    context: Option[Request]
  ): Request[Either[String, String], Any] =
    context.getOrElse(Request()).partial.method(method, url).contentType(contentType)
      .header(Header.accept(contentType)).body(request.unsafeArray)
}

case object SttpTransport {

  /**
   * HTTP request context.
   *
   * @see [[https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_2.13/latest/sttp/client3/RequestT.html API]]
   * @param partial partially constructed request
   */
  case class Request(
    partial: PartialRequest[Either[String, String], Any] = basicRequest
  )

  case object Request {
    implicit val defaultContext: Request = Request()
  }
}
