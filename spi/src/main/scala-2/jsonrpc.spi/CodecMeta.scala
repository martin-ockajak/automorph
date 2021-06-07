package jsonrpc.spi

import jsonrpc.spi.Message

trait CodecMeta[Node]:
  /**
   * Encode a value as a node.
   *
   * @param value value of given type
   * @tparam T value type
   * @return message format node
   */
  def encode[T](value: T): Node

  /**
   * Decode a value from a node.
   *
   * @param node message format node
   * @tparam T value type
   * @return value of given type
   */
  def decode[T](node: Node): T
