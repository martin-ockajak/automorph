package jsonrpc

import jsonrpc.spi.Effect
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC message transport layer.
 *
 * Used by JSON-RPC client to send requests and receive responses to and from a remot endpoint.
 *
 * @tparam Outcome computation outcome effect type
 * @tparam Context request context type
 */
trait JsonRpcTransport[Outcome[_], Context]:

  /**
   * Send a ''request'' to a remote JSON-RPC endpoint and retrieve the ''response''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param request request message
   * @param context request context
   * @return response
   */
  def call(request: ArraySeq.ofByte, context: Option[Context]): Outcome[ArraySeq.ofByte]

  /**
   * Send a ''request'' to a remote JSON-RPC endpoint without retrieving a response.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param request request message
   * @param context request context
   * @return nothing
   */
  def notify(request: ArraySeq.ofByte, context: Option[Context]): Outcome[Unit]
