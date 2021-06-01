package jsonrpc

import jsonrpc.spi.Effect
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC message transport layer.
 *
 * The transport can be used by a JSON-RPC client to send JSON-RPC requests and receive JSON-RPC responses to and from a remote endpoint.
 *
 * @tparam Outcome effectful computation outcome type
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
