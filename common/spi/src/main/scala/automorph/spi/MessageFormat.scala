package automorph.spi

import automorph.spi.Message
import scala.collection.immutable.ArraySeq

/**
 * Structured message format serialization/deserialization plugin.
 *
 * The underlying format must support storing arbitrarily nested structures of basic data types.
 *
 * @tparam Node message format node representation type
 */
trait MessageFormat[Node] extends FormatMeta[Node] {
  /**
   * Message format media (MIME) type.
   *
   * @return media (MIME) type
   */
  def mediaType: String

  /**
   * Serializes a message as binary data.
   *
   * @param message message
   * @return binary data in the specific format
   */
  def serialize(message: Message[Node]): ArraySeq.ofByte

  /**
   * Deserializes a message from binary data.
   *
   * @param data binary data in the specific format
   * @return message
   */
  def deserialize(data: ArraySeq.ofByte): Message[Node]

  /**
   * Formats a message as text.
   *
   * @param message message
   * @return message in textual form
   */
  def format(message: Message[Node]): String
}
