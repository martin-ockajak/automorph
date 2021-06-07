package jsonrpc.spi

import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq

trait CodecMeta[Node]:
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
