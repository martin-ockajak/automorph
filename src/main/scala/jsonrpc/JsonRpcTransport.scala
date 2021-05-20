package jsonrpc

import jsonrpc.spi.Effect
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC transport.
 *
 * @tparam Outcome computation outcome effect type
 */
trait JsonRpcTransport[Outcome[_]]:

  def call(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte]

  def notify(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte]
