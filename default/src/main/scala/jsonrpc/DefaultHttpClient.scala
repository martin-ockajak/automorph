package jsonrpc

import jsonrpc.Client
import jsonrpc.DefaultTypes.DefaultClient
import jsonrpc.backend.IdentityBackend.Identity
import jsonrpc.backend.{FutureBackend, IdentityBackend}
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.spi.Backend
import jsonrpc.transport.http.SttpTransport
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.SttpBackend
import sttp.model.{Method, Uri}

case object DefaultHttpClient {

  /**
   * Create a JSON-RPC over HTTP client using the specified ''backend'' plugin.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param backend effect backend plugin
   * @param url endpoint URL
   * @param httpMethod HTTP method
   * @param sttpBackend HTTP client backend
   * @tparam Effect effect type
   * @return JSON-RPC over HTTP client
   */
  def apply[Effect[_]](
    backend: Backend[Effect],
    url: Uri,
    httpMethod: Method,
    sttpBackend: SttpBackend[Effect, _]
  ): DefaultClient[Effect] = {
    val codec = UpickleJsonCodec()
    val transport = SttpTransport(url, httpMethod, codec.mediaType, sttpBackend, backend)
    Client(codec, backend, transport)
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
   * @param executionContext execution context
   * @return asynchronous JSON-RPC over HTTP client
   */
  def async(url: Uri, httpMethod: Method, sttpBackend: SttpBackend[Future, _])(implicit
    executionContext: ExecutionContext
  ): DefaultClient[Future] =
    DefaultHttpClient(FutureBackend(), url, httpMethod, sttpBackend)

  /**
   * Create a asynchronous JSON-RPC over HTTP client.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url endpoint URL
   * @param httpMethod HTTP method
   * @return synchronous JSON-RPC over HTTP client
   */
  def sync(url: Uri, httpMethod: Method, httpBackend: SttpBackend[Identity, _]): DefaultClient[Identity] =
    DefaultHttpClient(IdentityBackend(), url, httpMethod, httpBackend)
}
