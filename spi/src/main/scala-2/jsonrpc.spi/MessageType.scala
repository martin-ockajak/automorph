package jsonrpc.spi

/** JSON-RPC message types. */
object MessageType extends Enumeration {
  type MessageType = Value
  val Call, Notification, Result, Error = Value
}
