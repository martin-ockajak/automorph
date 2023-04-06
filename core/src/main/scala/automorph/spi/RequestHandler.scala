package automorph.spi

import automorph.spi.RequestHandler.Result
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
  def processRequest(body: InputStream, context: Context, id: String): Effect[Option[Result[Context]]]

  /** Message format media (MIME) type. */
  def mediaType: String
}

case object RequestHandler {

  /**
   * RPC handler request processing result.
   *
   * @param responseBody
   *   response message body
   * @param exception
   *   failed call exception
   * @param context
   *   response context
   * @tparam Context
   *   response context type
   */
  final case class Result[Context](
    responseBody: InputStream,
    exception: Option[Throwable],
    context: Option[Context],
  )

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
  private[automorph] def dummy[Effect[_], Context]: RequestHandler[Effect, Context] =
    new RequestHandler[Effect, Context] {
      def processRequest(
        body: InputStream,
        context: Context,
        id: String,
      ): Effect[Option[Result[Context]]] =
        throw new IllegalStateException("RPC request handler not initialized")

      /** Message format media (MIME) type. */
      override def mediaType: String =
        ""
    }
}
