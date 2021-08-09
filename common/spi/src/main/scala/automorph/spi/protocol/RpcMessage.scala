package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 * RPC message.
 *
 * @constructor Creates RPC message.
 * @param details protocol-specific message details
 * @param body message body
 * @param properties message properties
 * @param text message text representation
 * @tparam Details protocol-specific message details type
 */
final case class RpcMessage[Details](
  details: Details,
  body: ArraySeq.ofByte,
  properties: Map[String, String] = Map.empty,
  text: () => Option[String] = () => None
)
