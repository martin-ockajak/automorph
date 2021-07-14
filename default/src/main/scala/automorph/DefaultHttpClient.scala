package automorph

import automorph.backend.IdentityBackend.Identity
import automorph.codec.json.UpickleJsonCodec
import automorph.spi.{EffectSystem, ClientMessageTransport}
import automorph.transport.http.client.SttpTransport
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{HttpURLConnectionBackend, SttpBackend}

case object DefaultHttpClient {
  /**
   * Default client type.
   *
   * @tparam Effect effect type
   */
  type Type[Effect[_]] = Client[DefaultCodec.Node, DefaultCodec.Type, Effect, SttpTransport.Context]

  /** Request context type. */
  type Context = SttpTransport.Context

  /**
   * Creates a default JSON-RPC client using HTTP as message transport protocol with specified ''backend'' plugin.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param backend effect system plugin
   * @param transport message transport protocol plugin
   * @tparam Effect effect type
   * @tparam RequestContext request context type
   * @return client
   */
  def apply[Effect[_], RequestContext](
    backend: EffectSystem[Effect],
    transport: ClientMessageTransport[Effect, RequestContext]
  ): Client[DefaultCodec.Node, DefaultCodec.Type, Effect, RequestContext] = Client(UpickleJsonCodec(), backend, transport)

  /**
   * Creates a default JSON-RPC client using HTTP as message transport protocol with specified ''backend'' plugin.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param backend effect system plugin
   * @param sttpBackend HTTP client backend
   * @tparam Effect effect type
   * @return client
   */
  def apply[Effect[_]](
    url: String,
    method: String,
    backend: EffectSystem[Effect],
    sttpBackend: SttpBackend[Effect, _]
  ): Type[Effect] = DefaultHttpClient(backend, SttpTransport(new URL(url), method, backend, sttpBackend))

  /**
   * Creates a default asynchronous JSON-RPC client using HTTP as message transport protocol and 'Future' as an effect type.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param executionContext execution context
   * @return asynchronous client
   */
  def async(url: String, method: String)(implicit
    executionContext: ExecutionContext
  ): Type[Future] = DefaultHttpClient(url, method, DefaultBackend.async, AsyncHttpClientFutureBackend())

  /**
   * Creates a default asynchronous JSON-RPC client using HTTP as message transport protocol and identity as an effect type.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @see [[https://sttp.softwaremill.com/en/latest/index.html HTTP Client Documentation]]
   * @param url HTTP endpoint URL
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @return synchronous client
   */
  def sync(url: String, method: String): Type[Identity] = {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    DefaultHttpClient(url, method, DefaultBackend.sync, HttpURLConnectionBackend())
  }
}
