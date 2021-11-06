package automorph.protocol.restrpc

/**
 * REST-RPC API error exception.
 *
 * API methods bound via an RPC request handler can throw this exception to customize REST-RPC error response.
 *
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @constructor Creates a new REST-RPC error exception.
 * @param message error message
 * @param code error code
 * @param details additional error information
 * @param cause exception cause
 * @tparam Node message codec node representation type
 */
final case class RestRpcException[Node](
  message: String,
  code: Option[Int],
  details: Option[Node],
  cause: Throwable
) extends RuntimeException(message, cause)
