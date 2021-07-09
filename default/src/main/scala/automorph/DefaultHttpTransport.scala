package automorph

import automorph.DefaultTypes.DefaultTransport
import automorph.backend.IdentityBackend.Identity
import automorph.spi.Backend
import automorph.transport.http.SttpTransport
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

case object DefaultHttpTransport {

  /**
   * Creates a default message transport protocol plugin using HTTP as transport protocol.
   *
   * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend effectful computation backend plugin
   * @param sttpBackend HTTP client backend
   * @tparam Effect effect type
   * @return transport plugin
   */
  def apply[Effect[_]](
    url: String,
    method: String,
    backend: Backend[Effect],
    sttpBackend: SttpBackend[Effect, _]
  ): DefaultTransport[Effect] = SttpTransport(new URL(url), method, backend, sttpBackend)

  /**
   * Creates a default message transport protocol plugin using HTTP  as transport protocol and 'Future' as an effect type.
   *
   * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Transport Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param executionContext execution context
   * @return asynchronous transport plugin
   */
  def async(url: String, method: String)(implicit
    executionContext: ExecutionContext
  ): DefaultTransport[Future] = DefaultHttpTransport(url, method, DefaultBackend.async, AsyncHttpClientFutureBackend())

  /**
   * Creates a default message transport protocol plugin using HTTP  as transport protocol and identity as an effect type.
   *
   * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Transport Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @return synchronous transport plugin
   */
  def sync(url: String, method: String): DefaultTransport[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpTransport(url, method, DefaultBackend.sync, HttpURLConnectionBackend())
  }
}
