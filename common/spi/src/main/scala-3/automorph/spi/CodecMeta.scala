package automorph.spi

trait CodecMeta[Node]:
  /**
   * Encodes a value as a message format node.
   *
   * @param value value of given type
   * @tparam T value type
   * @return message format node
   */
  inline def encode[T](value: T): Node

  /**
   * Decodes a value from a message format node.
   *
   * @param node message format node
   * @tparam T value type
   * @return value of the specified type
   */
  inline def decode[T](node: Node): T
