package jsonrpc.transport.http.sttp

import java.net.{HttpURLConnection, URL, URLConnection}
import java.nio.charset.StandardCharsets
import jsonrpc.core.EncodingOps.asArraySeq
import jsonrpc.spi.{Effect, Transport}
import org.asynchttpclient.AsyncHttpClientConfig
import scala.collection.immutable.ArraySeq
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{emptyRequest, Empty, FollowRedirectsBackend, Identity, PartialRequest, Request, RequestOptions, RequestT, SttpApi, SttpBackend}
import sttp.model.{Header, Method, Uri}

/**
 * STTP HTTP transport using AsyncHttpClientFutureBackend.
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

  private val charset = StandardCharsets.UTF_8
  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val contentType = "application/json"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")

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
  ): RequestT[Identity, Either[String, String], Any] =
    context.copy[Identity, Either[String, String], Any](uri = url, method = method)
      .body(request.unsafeArray)
