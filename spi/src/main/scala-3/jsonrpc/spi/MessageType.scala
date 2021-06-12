package jsonrpc.spi

object MessageType:
  /** JSON-RPC message types. */
  enum MessageType:
    case Call, Notification, Result, Error
