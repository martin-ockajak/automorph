package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 * RPC service discovery operation.
 *
 * @constructor Creates RPC service discovery operation.
 * @param function service discovery RPC function description
 * @param specification creates API specification for supplied arguments and RPC functions
 */
final case class RpcDiscover(
  function: RpcFunction,
  specification: Seq[RpcFunction] => ArraySeq.ofByte
)
