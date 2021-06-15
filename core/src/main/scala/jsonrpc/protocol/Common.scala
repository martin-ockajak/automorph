package jsonrpc.protocol

/**
 * JSON-RPC protocol data structures.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 */
private[jsonrpc] case object Common {

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidRequest if the property value is missing
   */
  def mandatory[T](value: Option[T], name: String): T = value.getOrElse(
    throw Errors.InvalidRequest(s"Missing message property: $name", None.orNull)
  )
}
