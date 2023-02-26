package automorph

/**
 * RPC exception.
 *
 * @param message error message
 * @param cause error cause
 */
sealed abstract class RpcException(message: String, cause: Throwable = None.orNull)
  extends RuntimeException(message, cause)

object RpcException {

  /** Invalid RPC request error. */
  case class InvalidRequestException(message: String, cause: Throwable = None.orNull)
    extends RpcException(message, cause)

  /** Invalid RPC response error. */
  case class InvalidResponseException(message: String, cause: Throwable = None.orNull)
    extends RpcException(message, cause)

  /** Remote function not found error. */
  case class FunctionNotFoundException(message: String, cause: Throwable = None.orNull)
    extends RpcException(message, cause)

  /** Invalid remote function arguments error. */
  case class InvalidArgumentsException(message: String, cause: Throwable = None.orNull)
    extends RpcException(message, cause)

  /** Remote server error. */
  case class ServerErrorException(message: String, cause: Throwable = None.orNull)
    extends RpcException(message, cause)

  /** Remote API application error. */
  case class ApplicationErrorException(message: String, cause: Throwable = None.orNull)
    extends RpcException(message, cause)
}
