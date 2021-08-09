package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 * RPC message.
 *
 * @constructor Creates RPC message.
 * @param details protocol-specific message details
 * @param body message body
 * @param properties message properties
 * @param messageText textual message representation
 * @tparam Details protocol-specific message details type
 */
final case class RpcMessage[Details](
  details: Details,
  body: ArraySeq.ofByte,
  properties: Map[String, String] = Map.empty,
  private val messageText: () => Option[String] = () => None
) {
  /** Textual message representation. */
  lazy val text: Option[String] = messageText()
}
