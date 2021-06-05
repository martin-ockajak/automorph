package jsonrpc.spi

import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

/**
 * Hierarchical message format codec plugin.
 *
 * The underlying format must support storing arbitrarily nested structures of basic data types.
 *
 * @tparam Node message format node representation type
 */
trait Codec[Node]:
  /**
   * Message format MIME type.
   *
   * @return message format MIME type
   */
  def mimeType: String

  /**
   * Serialize a message as binary data.
   *
   * @param message message
   * @return binary data in the specific format
   */
  def serialize(message: Message[Node]): ArraySeq.ofByte

  /**
   * Deserialize a message from binary data.
   *
   * @param data binary data in the specific format
   * @return message
   */
  def deserialize(data: ArraySeq.ofByte): Message[Node]

  /**
   * Format a message as text.
   *
   * @param message message
   * @return message in textual form
   */
  def format(message: Message[Node]): String

  /**
   * Encode a value as a node.
   *
   * @param value value of given type
   * @tparam T value type
   * @return message format node
   */
  inline def encode[T](value: T): Node

  /**
   * Decode a value from a node.
   *
   * @param node message format node
   * @tparam T value type
   * @return value of given type
   */
  inline def decode[T](node: Node): T
