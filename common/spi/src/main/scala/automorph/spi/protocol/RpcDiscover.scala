package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 * RPC service discovery operation.
 *
 * @constructor Creates RPC service discovery operation.
 * @param function service discovery RPC function description
 * @param apiSpecification creates API specification for specified RPC functions and RPC request metadata
 * @tparam Metadata RPC message metadata
 */
final case class RpcDiscover[Metadata](
  function: RpcFunction,
  apiSpecification: (Seq[RpcFunction], Metadata) => ArraySeq.ofByte
)
