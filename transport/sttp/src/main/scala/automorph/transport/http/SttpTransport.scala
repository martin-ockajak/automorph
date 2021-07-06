package automorph.transport.http

import java.io.IOException
import automorph.spi.{Backend, Transport}
import scala.collection.immutable.ArraySeq
import sttp.client3.{Identity, PartialRequest, Request, Response, SttpApi, SttpBackend}
import sttp.model.{Header, Method, Uri}

/**
 * STTP HTTP transport using the specified STTP backend.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/ Documentation]]
 * @see [[https://www.javadoc.io/doc/com.softwaremill.sttp.client3/core_2.13/latest/sttp/client3/index.html API]]
 * @param url endpoint URL
 * @param method HTTP method
 * @param contentType HTTP request Content-Type
 * @param sttpBackend STTP backend
 * @param backend effect backend plugin
 * @tparam Effect effect type
 */
final case class SttpTransport[Effect[_]](
  url: Uri,
  method: Method,
  contentType: String,
  sttpBackend: SttpBackend[Effect, _],
  backend: Backend[Effect]
) extends Transport[Effect, PartialRequest[Either[String, String], Any]] with SttpApi {

  override def call(
    request: ArraySeq.ofByte,
    context: Option[PartialRequest[Either[String, String], Any]]
  ): Effect[ArraySeq.ofByte] = {
    val httpRequest = setupHttpRequest(request, context).response(asByteArray)
    backend.flatMap(
      httpRequest.send(sttpBackend),
      (response: Response[Either[String, Array[Byte]]]) => response.body.fold(
        error => backend.failed(new IOException(error)),
        response => backend.pure(new ArraySeq.ofByte(response))
      )
    )
  }

  override def notify(
    request: ArraySeq.ofByte,
    context: Option[PartialRequest[Either[String, String], Any]]
  ): Effect[Unit] = {
    val httpRequest = setupHttpRequest(request, context).response(ignore)
    backend.map(httpRequest.send(sttpBackend), (_: Response[Unit]) => ())
  }

  private def setupHttpRequest(
    request: ArraySeq.ofByte,
    context: Option[PartialRequest[Either[String, String], Any]]
  ): Request[Either[String, String], Any] = {
    context.getOrElse(basicRequest).copy[Identity, Either[String, String], Any](uri = url, method = method)
      .contentType(contentType).header(Header.accept(contentType)).body(request.unsafeArray)
  }
}
