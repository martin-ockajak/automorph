package jsonrpc.spi

import jsonrpc.spi.Effect
import scala.collection.immutable.ArraySeq

/**
 * Request and response message transport plugin.
 *
 * The transport is used by a client to send requests and receive responses to and from a remote endpoint.
 *
 * @tparam Outcome effectful computation outcome type
 * @tparam Context request context type
 */
trait Transport[Outcome[_], Context]:

  /**
   * Send a ''request'' to a remote endpoint and retrieve the ''response''.
   *
   * The specified ''context'' is used to supply additional information needed to send the request.
   *
   * @param request request message
   * @param context request context
   * @return response message
   */
  def call(request: ArraySeq.ofByte, context: Context): Outcome[ArraySeq.ofByte]

  /**
   * Send a ''request'' to a remote endpoint without retrieving a response.
   *
   * The specified ''context'' is used to supply additional information needed to send the request.
   *
   * @param request request message
   * @param context request context
   * @return nothing
   */
  def notify(request: ArraySeq.ofByte, context: Context): Outcome[Unit]
