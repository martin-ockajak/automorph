package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 * RPC message.
 *
 * @constructor Creates RPC message.
 * @param content protocol-specific message content
 * @param body message body
 * @param properties message properties
 * @param text message text representation
 * @tparam Content protocol-specific message content type
 */
final case class RpcMessage[Content](
  content: Content,
  body: ArraySeq.ofByte,
  properties: Map[String, String] = Map.empty,
  text: () => Option[String] = () => None
)
