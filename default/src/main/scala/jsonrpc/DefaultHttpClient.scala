package jsonrpc

import jsonrpc.Client
import jsonrpc.backend.FutureBackend
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.spi.Backend
import jsonrpc.transport.http.SttpTransport
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.{PartialRequest, SttpBackend}
import sttp.model.{Method, Uri}
import ujson.Value

case object DefaultHttpClient {

  /**
   * Default JSON-RPC over HTTP client.
   *
   * The client can be used by an application to perform JSON-RPC calls and notifications.
   *
   * @param url endpoint URL
   * @param httpMethod HTTP method
   * @param sttpBackend STTP backend
   * @param backend effect backend plugin
   * @tparam Effect effect type
   * @return JSON-RPC over HTTP client
   */
  def apply[Effect[_]](
    url: Uri,
    httpMethod: Method,
    sttpBackend: SttpBackend[Effect, _],
    backend: Backend[Effect]
  ): Client[Value, UpickleJsonCodec[UpickleCustom], Effect, PartialRequest[Either[String, String], Any]] = {
    val codec = UpickleJsonCodec()
    val transport = SttpTransport(url, httpMethod, codec.mediaType, sttpBackend, backend)
    Client[Value, UpickleJsonCodec[UpickleCustom], Effect, PartialRequest[Either[String, String], Any]](
      codec,
      backend,
      transport
    )
  }

  /**
   * Default asynchronous JSON-RPC over HTTP client.
   *
   * The client can be used by an application to perform JSON-RPC calls and notifications.
   *
   * @param url endpoint URL
   * @param httpMethod HTTP method
   * @param sttpBackend STTP backend
   * @return asynchronous JSON-RPC over HTTP client
   */
  def apply(
    url: Uri,
    httpMethod: Method,
    sttpBackend: SttpBackend[Future, _]
  )(implicit executionContext: ExecutionContext): Client[Value, UpickleJsonCodec[UpickleCustom], Future, PartialRequest[Either[String, String], Any]] = {
    val backend = FutureBackend()
    DefaultHttpClient(url, httpMethod, sttpBackend, backend)
  }
}
