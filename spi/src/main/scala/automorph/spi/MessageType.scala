package automorph.spi

/** JSON-RPC message types. */
sealed abstract class MessageType {
  def name: String = toString
}

object MessageType {
  case object Call extends MessageType
  case object Notification extends MessageType
  case object Result extends MessageType
  case object Error extends MessageType
}
