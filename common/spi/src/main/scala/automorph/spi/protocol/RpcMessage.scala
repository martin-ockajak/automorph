package automorph.spi.protocol

import scala.collection.immutable.ArraySeq

/**
 * RPC message.
 *
 * @constructor Creates RPC message.
 * @param details protocol-specific message details
 * @param body message body
 * @param properties message properties
 * @param text message in human-readable textual form
 * @tparam Details protocol-specific message details type
 */
final case class RpcMessage[Details](
  details: Details,
  body: ArraySeq.ofByte,
  properties: Map[String, String] = Map.empty,
  private val messageText: () => Option[String] = () => None
) {
  /** Message in human-readable textual form. */
  lazy val text: Option[String] = messageText()
}
