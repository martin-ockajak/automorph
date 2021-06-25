package jsonrpc.transport.http

import jsonrpc.spi.{Backend, Transport}
import scala.collection.immutable.ArraySeq
import sttp.client3.{Identity, PartialRequest, Request, Response, SttpApi, SttpBackend}
import sttp.model.{Header, Method, Uri}

/**
 * STTP HTTP transport using the specified STTP backend.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/index.html Documentation]]
 * @param url endpoint URL
 * @param method HTTP method
 * @param contentType HTTP request Content-Type
 * @param sttpBackend STTP backend
 * @param backend effect backend plugin
 * @tparam Effect effect type
 */
case class SttpTransport[Effect[_]](
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
      response => response.body.fold(
        error => backend.failed(IllegalStateException(error)),
        response => backend.pure(ArraySeq.ofByte(response))
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
