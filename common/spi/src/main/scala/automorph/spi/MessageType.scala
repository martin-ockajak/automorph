package automorph.spi

/**
 * RPC message type.
 */
sealed abstract class MessageType {
  /**
   * Message type name.
   *
   * @return message type name
   */
  def name: String = toString
}

object MessageType {

  /** JSON-RPC method call request. */
  case object Call extends MessageType
  /** JSON-RPC method notification request. */
  case object Notification extends MessageType
  /** JSON-RPC result response. */
  case object Result extends MessageType
  /** JSON-RPC error response. */
  case object Error extends MessageType
}
