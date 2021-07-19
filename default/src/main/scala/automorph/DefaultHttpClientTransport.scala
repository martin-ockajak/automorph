package automorph

import automorph.system.IdentitySystem.Identity
import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.transport.http.client.SttpClient
import java.net.URI
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

case object DefaultHttpClientTransport {

  /**
   * Default message transport type.
   *
   * @tparam Effect effect type
   */
  type Type[Effect[_]] = ClientMessageTransport[Effect, SttpClient.Context]

  /** Request context type. */
  type Context = SttpClient.Context

  /**
   * Creates a default message transport protocol plugin using HTTP as transport protocol.
   *
   * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend effect system plugin
   * @param sttpSystem HTTP client backend
   * @tparam Effect effect type
   * @return transport plugin
   */
  def apply[Effect[_]](
    url: URI,
    method: String,
    backend: EffectSystem[Effect],
    sttpSystem: SttpBackend[Effect, _]
  ): Type[Effect] = SttpClient(url, method, backend, sttpSystem)

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
  def async(url: URI, method: String)(implicit
    executionContext: ExecutionContext
  ): Type[Future] = DefaultHttpClientTransport(url, method, DefaultEffectSystem.async, AsyncHttpClientFutureBackend())

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
  def sync(url: URI, method: String): Type[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClientTransport(url, method, DefaultEffectSystem.sync, HttpURLConnectionBackend())
  }
}
