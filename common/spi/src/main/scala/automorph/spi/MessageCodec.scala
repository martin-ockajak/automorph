package automorph.spi

import automorph.spi.Message
import scala.collection.immutable.ArraySeq

/**
 * Structured message format codec plugin.
 *
 * The underlying codec must support storing arbitrarily nested structures of basic data types.
 *
 * @tparam Node message codec node representation type
 */
trait MessageCodec[Node] extends CodecMeta[Node] {
  /**
   * Message codec media (MIME) type.
   *
   * @return media (MIME) type
   */
  def mediaType: String

  /**
   * Serializes a message as binary data.
   *
   * @param message message
   * @return binary data in the specific codec
   */
  def serialize(message: Message[Node]): ArraySeq.ofByte

  /**
   * Deserializes a message from binary data.
   *
   * @param data binary data in the specific codec
   * @return message
   */
  def deserialize(data: ArraySeq.ofByte): Message[Node]

  /**
   * Serializes a node as binary data.
   *
   * @param node node
   * @return binary data in the specific codec
   */
  def serializeNode(message: Node): ArraySeq.ofByte

  /**
   * Deserializes a node from binary data.
   *
   * @param data binary data in the specific codec
   * @return node
   */
  def deserializeNode(data: ArraySeq.ofByte): Node

  /**
   * Codecs a message as human-readable text.
   *
   * @param message message
   * @return message in human-readable textual form
   */
  def text(message: Message[Node]): String
}
