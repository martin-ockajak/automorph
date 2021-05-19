package jsonrpc.spi

/**
 * JSON-RPC protocol message structure.
 * Specification: https://www.jsonrpc.org/specification
 *
 * @param jsonrpc protocol version (must be 2.0)
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @param result succesful call result value
 * @param error failed call error details
 * @tparam Node message node representation type
 */
final case class Message[Node](
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Either[List[Node], Map[String, Node]]],
  result: Option[Node],
  error: Option[CallError[Node]]
)

/**
 * JSON-RPC protocol error details structure.
 * Specification: https://www.jsonrpc.org/specification
 *
 * @param code error code
 * @param message error description
 * @param data additional error information
 * @tparam Node message node representation type
 */
final case class CallError[Node](
  code: Option[Int],
  message: Option[String],
  data: Option[Node]
)
