package automorph

import automorph.Client
import automorph.DefaultTypes.DefaultClient
import automorph.backend.IdentityBackend.Identity
import automorph.backend.{FutureBackend, IdentityBackend}
import automorph.codec.json.UpickleJsonCodec
import automorph.spi.Backend
import automorph.transport.http.SttpTransport
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

case object DefaultHttpClient {

  /**
   * Creates a JSON-RPC client using HTTP as message transport protocol with specified ''backend'' plugin.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param url HTTP endpoint URL
   * @param httpMethod HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend effectful computation backend plugin
   * @param sttpBackend HTTP client backend
   * @tparam Effect effect type
   * @return JSON-RPC over HTTP client
   */
  def apply[Effect[_]](
    url: String,
    httpMethod: String,
    backend: Backend[Effect],
    sttpBackend: SttpBackend[Effect, _]
  ): DefaultClient[Effect] = {
    val codec = UpickleJsonCodec()
    val transport = SttpTransport(new URL(url), httpMethod, backend, sttpBackend)
    Client(codec, backend, transport)
  }

  /**
   * Creates an asynchronous JSON-RPC client using HTTP as message transport protocol and 'Future' as an effect type.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param httpMethod HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param executionContext execution context
   * @return asynchronous JSON-RPC over HTTP client
   */
  def async(url: String, httpMethod: String)(implicit
    executionContext: ExecutionContext
  ): DefaultClient[Future] =
    DefaultHttpClient(url, httpMethod, FutureBackend(), AsyncHttpClientFutureBackend())

  /**
   * Creates a asynchronous JSON-RPC client using HTTP as message transport protocol and identity as an effect type.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param httpMethod HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @return synchronous JSON-RPC over HTTP client
   */
  def sync(url: String, httpMethod: String): DefaultClient[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClient(url, httpMethod, IdentityBackend(), HttpURLConnectionBackend())
  }
}
