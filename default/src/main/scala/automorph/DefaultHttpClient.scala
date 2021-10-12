package automorph

import automorph.spi.EffectSystem
import automorph.system.IdentitySystem.Identity
import java.net.URI
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

object DefaultHttpClient {

  /**
   * Default client type.
   *
   * @tparam Effect effect type
   */
  type Type[Effect[_]] = Client[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Effect, Context]

  /** Request context type. */
  type Context = DefaultHttpClientTransport.Context

  /**
   * Creates a JSON-RPC over STTP HTTP client with specified effect system plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend STTP client backend
   * @param system effect system plugin
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @tparam Effect effect type
   * @return RPC client
   */
  def apply[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, _],
    system: EffectSystem[Effect],
    webSocket: Boolean = false
  ): Type[Effect] = DefaultClient(DefaultHttpClientTransport(url, method, backend, system))

  /**
   * Creates an asynchronous JSON-RPC over STTP HTTP client using 'Future' as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @param executionContext execution context
   * @return asynchronous RPC client
   */
  def async(url: URI, method: String, webSocket: Boolean = false)(implicit
    executionContext: ExecutionContext
  ): Type[Future] = DefaultHttpClient(url, method, AsyncHttpClientFutureBackend(), DefaultEffectSystem.async)

  /**
   * Creates a synchronous JSON-RPC over STTP HTTP client using identity as an effect type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @return synchronous RPC client
   */
  def sync(url: URI, method: String, webSocket: Boolean = false): Type[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClient(url, method, HttpURLConnectionBackend(), DefaultEffectSystem.sync)
  }
}
