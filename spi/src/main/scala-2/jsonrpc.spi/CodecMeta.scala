package jsonrpc.spi

import scala.annotation.nowarn

@nowarn
trait CodecMeta[Node] {
  /**
   * Encode a value as a node.
   *
   * @param value value of given type
   * @tparam T value type
   * @return message format node
   */
  def encode[T](value: T): Node = throw new UnsupportedOperationException("Macro not implemented")

  /**
   * Decode a value from a node.
   *
   * @param node message format node
   * @tparam T value type
   * @return value of given type
   */
  def decode[T](node: Node): T = throw new UnsupportedOperationException("Macro not implemented")
}
