package automorph.protocol.jsonrpc

import automorph.protocol.jsonrpc.ErrorType
import automorph.protocol.jsonrpc.ErrorType.{InternalErrorException, ParseErrorException, ServerErrorException}
import automorph.spi.RpcProtocol.{FunctionNotFoundException, InvalidRequestException}

/** JSON-RPC protocol errors. */
private[automorph] trait ErrorMapping {

  /**
   * Maps a JSON-RPC error to a corresponding default exception.
   *
   * @param message error message
   * @param code error code
   * @return exception
   */
  def defaultMapError(message: String, code: Int): Throwable = code match {
    case ErrorType.ParseError.code => ParseErrorException(message)
    case ErrorType.InvalidRequest.code => InvalidRequestException(message)
    case ErrorType.MethodNotFound.code => FunctionNotFoundException(message)
    case ErrorType.InvalidParams.code => new IllegalArgumentException(message)
    case ErrorType.InternalError.code => InternalErrorException(message)
    case _ if Range(ErrorType.ReservedError.code, ErrorType.ServerError.code + 1).contains(code) =>
      ServerErrorException(message)
    case _ => new RuntimeException(message)
  }

  /**
   * Maps an exception to a corresponding default JSON-RPC error type.
   *
   * @param exception exception
   * @return JSON-RPC error type
   */
  def defaultMapException(exception: Throwable): ErrorType = exception match {
    case _: ParseErrorException => ErrorType.ParseError
    case _: InvalidRequestException => ErrorType.InvalidRequest
    case _: FunctionNotFoundException => ErrorType.MethodNotFound
    case _: IllegalArgumentException => ErrorType.InvalidParams
    case _: InternalErrorException => ErrorType.InternalError
    case _: ServerErrorException => ErrorType.ServerError
    case _ => ErrorType.ApplicationError
  }
}
