package automorph.spi

import java.io.InputStream

/**
 * RPC request handler.
 *
 * Processes remote API requests and invoke bound API methods.
 *
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   RPC message context type
 */
trait RequestHandler[Effect[_], Context] {

  /**
   * Processes an RPC request by invoking a bound remote function based on the specified RPC request
   * along with request context and return an RPC response.
   *
   * @param body
   *   request message body
   * @param context
   *   request context
   * @param id
   *   request correlation identifier
   * @return
   *   request processing result
   */
  def processRequest(body: InputStream, context: Context, id: String): Effect[Option[RpcResult[Context]]]

  /** Message format media (MIME) type. */
  def mediaType: String
}

object RequestHandler {
  /**
   * Dummy RPC request handler.
   *
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   RPC message context type
   * @return
   *   dummy RPC request handler
   */
  def dummy[Effect[_], Context]: RequestHandler[Effect, Context] =
    new RequestHandler[Effect, Context] {
      def processRequest(
        body: InputStream,
        context: Context,
        id: String,
      ): Effect[Option[RpcResult[Context]]] =
        throw new IllegalStateException("RPC request handler not initialized")

      /** Message format media (MIME) type. */
      override def mediaType: String =
        ""
    }
}
