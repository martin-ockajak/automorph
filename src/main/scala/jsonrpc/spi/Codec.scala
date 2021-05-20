package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

/**
 * Hierarchical data format codec plugin.
 *
 * @tparam Node data format node representation type
 */
trait Codec[Node]:
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
   * @return data format node
   */
  inline def encode[T](value: T): Node

  /**
   * Decode a value from a node.
   *
   * @param node data format node
   * @tparam T value type
   * @return value of given type
   */
  inline def decode[T](node: Node): T
