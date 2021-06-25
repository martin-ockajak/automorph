package jsonrpc.spi

/** JSON-RPC message types. */
sealed abstract class MessageType(val name: String)

object MessageType {
  case object Call extends MessageType("Call")
  case object Notification extends MessageType("Notification")
  case object Result extends MessageType("Result")
  case object Error extends MessageType("Error")
}
