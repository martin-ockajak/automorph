package jsonrpc.transport.http.sttp

import jsonrpc.core.EncodingOps.asArraySeq
import jsonrpc.spi.{Effect, Transport}
import scala.collection.immutable.ArraySeq
import sttp.client3.{Identity, PartialRequest, Request, SttpApi, SttpBackend}
import sttp.model.{Header, MediaType, Method, Uri}

/**
 * STTP HTTP transport using the specified STTP backend.
 *
 * @see [[https://sttp.softwaremill.com/en/latest/index.html Documentation]]
 * @param url HTTP endpoint URL
 * @param method HTTP method
 * @param backend STTP backend
 * @param effect effect system plugin
 */
case class SttpTransport[Outcome[_], Capabilities](
  url: Uri,
  method: Method,
  backend: SttpBackend[Outcome, Capabilities],
  effect: Effect[Outcome]
) extends Transport[Outcome, PartialRequest[Either[String, String], Any]] with SttpApi:

  private val contentType = MediaType.ApplicationJson

  override def call(
    request: ArraySeq.ofByte,
    context: PartialRequest[Either[String, String], Any]
  ): Outcome[ArraySeq.ofByte] =
    val httpRequest = setupHttpRequest(request, context).response(asByteArray)
    effect.flatMap(
      httpRequest.send(backend),
      _.body.fold(
        error => effect.failed(IllegalStateException(error)),
        response => effect.pure(response.asArraySeq)
      )
    )

  override def notify(
    request: ArraySeq.ofByte,
    context: PartialRequest[Either[String, String], Any]
  ): Outcome[Unit] =
    val httpRequest = setupHttpRequest(request, context).response(ignore)
    effect.map(httpRequest.send(backend), _.body)

  private def setupHttpRequest(
    request: ArraySeq.ofByte,
    context: PartialRequest[Either[String, String], Any]
  ): Request[Either[String, String], Any] =
    context.copy[Identity, Either[String, String], Any](uri = url, method = method)
      .contentType(contentType).header(Header.accept(contentType)).body(request.unsafeArray)
