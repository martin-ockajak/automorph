package jsonrpc

import jsonrpc.spi.Effect
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC message transport layer.
 *
 * Used to send and receive messages to and from a remote JSON-RPC endpoint.
 *
 * @tparam Outcome computation outcome effect type
 */
trait JsonRpcTransport[Outcome[_]]:

  /**
   * Send a request to a remote JSON-RPC endpoint and retrieves the response.
   *
   * @param request request message
   * @return response
   */
  def call(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte]

  /**
   * Send a request to a remote JSON-RPC endpoint without retrieving a response.
   *
   * @param request request message
   * @return nothing
   */
  def notify(request: ArraySeq.ofByte): Outcome[Unit]
