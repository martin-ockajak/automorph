package automorph

import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem.Identity
import automorph.transport.http.client.SttpClient
import java.net.URI
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

object DefaultHttpClientTransport {

  /**
   * Default message transport type.
   *
   * @tparam Effect effect type
   */
  type Type[Effect[_]] = ClientMessageTransport[Effect, Context]

  /** Request context type. */
  type Context = SttpClient.Context

  /**
   * Creates an STTP HTTP client message transport plugin with specified effect system plugin.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend client message transport backend
   * @param system effect system plugin
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @tparam Effect effect type
   * @return client message transport plugin
   */
  def apply[Effect[_]](
    url: URI,
    method: String,
    backend: SttpBackend[Effect, _],
    system: EffectSystem[Effect],
    webSocket: Boolean = false
  ): Type[Effect] = DefaultHttpClientTransport(url, method, backend, system)

  /**
   * Creates an asynchronous STTP HTTP client message transport plugin using 'Future' as an effect type.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @param executionContext execution context
   * @return asynchronous client message transport plugin
   */
  def async(url: URI, method: String, webSocket: Boolean = false)(implicit
    executionContext: ExecutionContext
  ): Type[Future] =
    DefaultHttpClientTransport(url, method, AsyncHttpClientFutureBackend(), DefaultEffectSystem.async)

  /**
   * Creates a synchronous STTP HTTP client message transport plugin using identity as an effect type.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://sttp.softwaremill.com/en/latest Library documentation]]
   * @see [[https://www.javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/tapir/index.html API]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param webSocket upgrade HTTP connections to use WebSocket protocol if true, use HTTP if false
   * @return synchronous client message transport plugin
   */
  def sync(url: URI, method: String, webSocket: Boolean = false): Type[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClientTransport(url, method, HttpURLConnectionBackend(), DefaultEffectSystem.sync)
  }
}
