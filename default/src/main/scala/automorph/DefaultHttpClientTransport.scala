package automorph

import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem.Identity
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
  type Type[Effect[_]] = ClientMessageTransport[Effect, Context]

  /** Request context type. */
  type Context = SttpClient.Context

  /**
   * Creates a default client message transport protocol plugin using HTTP as messge transport protocol with specified effect ''system'' plugin.
   *
   * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
   *
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend client message transport backend
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return transport plugin
   */
  def apply[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, _],
    system: EffectSystem[Effect]
  ): Type[Effect] = DefaultHttpClientTransport(url, method, backend, system)

  /**
   * Creates a default client message transport protocol plugin using HTTP as message transport protocol and 'Future' as an effect type.
   *
   * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
   *
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Transport Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param executionContext execution context
   * @return asynchronous transport plugin
   */
  def async(url: URI, method: String)(implicit executionContext: ExecutionContext): Type[Future] =
    DefaultHttpClientTransport(url, method, AsyncHttpClientFutureBackend(), DefaultEffectSystem.async)

  /**
   * Creates a default client message transport protocol plugin using HTTP as message transport protocol and identity as an effect type.
   *
   * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
   *
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Transport Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @return synchronous transport plugin
   */
  def sync(url: URI, method: String): Type[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClientTransport(url, method, HttpURLConnectionBackend(), DefaultEffectSystem.sync)
  }
}
