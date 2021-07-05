package jsonrpc

import jsonrpc.Client
import jsonrpc.DefaultTypes.DefaultClient
import jsonrpc.backend.IdentityBackend.Identity
import jsonrpc.backend.{FutureBackend, IdentityBackend}
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.spi.Backend
import jsonrpc.transport.http.SttpTransport
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
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
    sttpBackend: SttpBackend[Effect, _],
    url: Uri,
    httpMethod: Method = Method.POST
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
   * @param method HTTP method
   * @param executionContext execution context
   * @return asynchronous JSON-RPC over HTTP client
   */
  def async(url: Uri, method: Method)(implicit
    executionContext: ExecutionContext
  ): DefaultClient[Future] =
    DefaultHttpClient(FutureBackend(), AsyncHttpClientFutureBackend(), url, method)

  /**
   * Create a asynchronous JSON-RPC over HTTP client.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url endpoint URL
   * @param method HTTP method
   * @return synchronous JSON-RPC over HTTP client
   */
  def sync(url: Uri, method: Method): DefaultClient[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClient(IdentityBackend(), HttpURLConnectionBackend(), url, method)
  }
}
