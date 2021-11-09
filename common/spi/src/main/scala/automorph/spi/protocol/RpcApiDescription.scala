package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 * RPC API description operation.
 *
 * @constructor Creates RPC API description operation.
 * @param function RPC function description
 * @param invoke creates API description for specified RPC functions and RPC request metadata
 * @tparam Node message node type
 */
final case class RpcApiDescription[Node](
  function: RpcFunction,
  invoke: Iterable[RpcFunction] => Node
)
