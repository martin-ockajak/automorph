package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 *
 * @constructor Creates RPC service discovery operation.
 * @param details protocol-specific message details
 * @param body message body
 * @param properties message properties
 * @param text message in human-readable textual form
 * @tparam Details protocol-specific message details type
 */

/**
 * RPC service discovery operation.
 *
 * @param function service discovery RPC function
 * @param specification creates API specification for supplied RPC functions
 */
final case class RpcDiscover(
  function: RpcFunction,
  specification: Seq[RpcFunction] => ArraySeq.ofByte
)
