package jsonrpc.spi

/** JSON-RPC message types. */
enum MessageType:
  case Call, Notification, Result, Error
