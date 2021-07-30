package automorph

import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.system.IdentitySystem.Identity
import java.net.URI
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

case object DefaultHttpClient {

  /**
   * Default client type.
   *
   * @tparam Effect effect type
   */
  type Type[Effect[_]] = Client[DefaultMessageFormat.Node, DefaultMessageFormat.Type, Effect, Context]

  /** Request context type. */
  type Context = DefaultHttpClientTransport.Context

  /**
   * Creates a default RPC client using HTTP as message transport protocol with specified effect ''system'' plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend STTP client backend
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return RPC client
   */
  def apply[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, _],
    system: EffectSystem[Effect]
  ): Type[Effect] = DefaultClient(system, DefaultHttpClientTransport(url, method, backend, system))

  /**
   * Creates a default asynchronous RPC client using HTTP as message transport protocol and 'Future' as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param executionContext execution context
   * @return asynchronous RPC client
   */
  def async(url: URI, method: String)(implicit
    executionContext: ExecutionContext
  ): Type[Future] = DefaultHttpClient(url, method, AsyncHttpClientFutureBackend(), DefaultEffectSystem.async)

  /**
   * Creates a default asynchronous RPC client using HTTP as message transport protocol and identity as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @return synchronous RPC client
   */
  def sync(url: URI, method: String): Type[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClient(url, method, HttpURLConnectionBackend(), DefaultEffectSystem.sync)
  }
}
