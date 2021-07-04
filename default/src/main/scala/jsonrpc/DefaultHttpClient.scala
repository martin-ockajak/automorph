package jsonrpc

import jsonrpc.Client
import jsonrpc.backend.{FutureBackend, IdentityBackend}
import jsonrpc.backend.IdentityBackend.Identity
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
   * Create a JSON-RPC over HTTP client using the specified ''backend'' plugin.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param url endpoint URL
   * @param httpMethod HTTP method
   * @param httpBackend HTTP client backend
   * @param backend effect backend plugin
   * @tparam Effect effect type
   * @return JSON-RPC over HTTP client
   */
  def apply[Effect[_]](
    url: Uri,
    httpMethod: Method,
    httpBackend: SttpBackend[Effect, _],
    backend: Backend[Effect]
  ): Client[Value, UpickleJsonCodec[UpickleCustom], Effect, PartialRequest[Either[String, String], Any]] = {
    val codec = UpickleJsonCodec()
    val transport = SttpTransport(url, httpMethod, codec.mediaType, httpBackend, backend)
    Client[Value, UpickleJsonCodec[UpickleCustom], Effect, PartialRequest[Either[String, String], Any]](
      codec,
      backend,
      transport
    )
  }

  /**
   * Create an asynchronous JSON-RPC over HTTP client.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url endpoint URL
   * @param httpMethod HTTP method
   * @param httpBackend HTTP client backend
   * @param executionContext execution context
   * @return asynchronous JSON-RPC over HTTP client
   */
  def async(url: Uri, httpMethod: Method, httpBackend: SttpBackend[Future, _])(
    implicit executionContext: ExecutionContext
  ): Client[Value, UpickleJsonCodec[UpickleCustom], Future, PartialRequest[Either[String, String], Any]] =
    DefaultHttpClient(url, httpMethod, httpBackend, FutureBackend())

  /**
   * Create a asynchronous JSON-RPC over HTTP client.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url endpoint URL
   * @param httpMethod HTTP method
   * @param httpBackend HTTP client backend
   * @return synchronous JSON-RPC over HTTP client
   */
  def sync(
    url: Uri,
    httpMethod: Method,
    httpBackend: SttpBackend[Identity, _]
  ): Client[Value, UpickleJsonCodec[UpickleCustom], Identity, PartialRequest[Either[String, String], Any]] =
    DefaultHttpClient(url, httpMethod, httpBackend, IdentityBackend())
}
