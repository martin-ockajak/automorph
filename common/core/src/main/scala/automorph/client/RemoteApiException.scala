package automorph.client

/**
 * Remote API error exception.
 *
 * Thrown by an RPC client if RPC protocol response indicates an application error.
 *
 * @constructor
 *   Creates a new remote API error exception.
 * @param message
 *   error message
 * @param cause
 *   exception cause
 */
final case class RemoteApiException(message: String, cause: Throwable)
  extends RuntimeException(message, cause)
