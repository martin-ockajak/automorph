package automorph

import automorph.system.IdentitySystem.Identity
import automorph.spi.{ClientMessageTransport, EffectSystem}
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
   * Creates a default RPC client using HTTP as message transport protocol with specified ''backend'' plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param system effect system plugin
   * @param transport message transport protocol plugin
   * @tparam Effect effect type
   * @tparam RequestContext request context type
   * @return RPC client
   */
  def apply[Effect[_], RequestContext](
    system: EffectSystem[Effect],
    transport: ClientMessageTransport[Effect, RequestContext]
  ): Client[DefaultMessageFormat.Node, DefaultMessageFormat.Type, Effect, RequestContext] =
    Client(DefaultMessageFormat(), system, transport)

  /**
   * Creates a default RPC client using HTTP as message transport protocol with specified ''backend'' plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param system effect system plugin
   * @param backend HTTP client backend
   * @tparam Effect effect type
   * @return RPC client
   */
  def apply[Effect[_]](
    url: URI,
    method: String,
    system: EffectSystem[Effect],
    backend: SttpBackend[Effect, _]
  ): Type[Effect] = DefaultHttpClient(system, DefaultHttpClientTransport(url, method, system, backend))

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
  ): Type[Future] = DefaultHttpClient(url, method, DefaultEffectSystem.async, AsyncHttpClientFutureBackend())

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
    DefaultHttpClient(url, method, DefaultEffectSystem.sync, HttpURLConnectionBackend())
  }
}
