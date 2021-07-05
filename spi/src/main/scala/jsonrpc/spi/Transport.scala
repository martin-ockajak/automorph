package jsonrpc.spi

import scala.collection.immutable.ArraySeq

/**
 * Message transport plugin.
 *
 * The transport can be used by a JSON-RPC client to send requests and receive responses to and from a remote endpoint.
 *
 * @tparam Effect effect type
 * @tparam Context request context type
 */
trait Transport[Effect[_], Context] {

  /**
   * Send a ''request'' to a remote endpoint and retrieve the ''response''.
   *
   * The optional ''request context'' is used to supply additional information needed to send the request.
   *
   * @param request request message
   * @param context request context
   * @return response message
   */
  def call(request: ArraySeq.ofByte, context: Option[Context]): Effect[ArraySeq.ofByte]

  /**
   * Send a ''request'' to a remote endpoint without retrieving a response.
   *
   * The optional ''request context'' is used to supply additional information needed to send the request.
   *
   * @param request request message
   * @param context request context
   * @return nothing
   */
  def notify(request: ArraySeq.ofByte, context: Option[Context]): Effect[Unit]
}