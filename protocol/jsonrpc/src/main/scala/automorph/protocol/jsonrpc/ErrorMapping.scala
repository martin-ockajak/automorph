package automorph.protocol.jsonrpc

import automorph.RpcException.{
  ApplicationErrorException, FunctionNotFoundException, InvalidArgumentsException, InvalidRequestException,
  ServerErrorException,
}

/** JSON-RPC protocol errors. */
private[automorph] trait ErrorMapping {

  /**
   * Maps a JSON-RPC error to a corresponding default exception.
   *
   * @param message
   *   error message
   * @param code
   *   error code
   * @return
   *   exception
   */
  def defaultMapError(message: String, code: Int): Throwable =
    code match {
      case ErrorType.ParseError.code => InvalidRequestException(message)
      case ErrorType.InvalidRequest.code => InvalidRequestException(message)
      case ErrorType.MethodNotFound.code => FunctionNotFoundException(message)
      case ErrorType.InvalidParams.code => InvalidArgumentsException(message)
      case ErrorType.InternalError.code => ServerErrorException(message)
      case _ if Range(ErrorType.ReservedError.code, ErrorType.ServerError.code + 1).contains(code) =>
        ServerErrorException(message)
      case _ => ApplicationErrorException(message)
    }

  /**
   * Maps an exception to a corresponding default JSON-RPC error type.
   *
   * @param exception
   *   exception
   * @return
   *   JSON-RPC error type
   */
  def defaultMapException(exception: Throwable): ErrorType =
    exception match {
      case _: InvalidRequestException => ErrorType.InvalidRequest
      case _: FunctionNotFoundException => ErrorType.MethodNotFound
      case _: IllegalArgumentException => ErrorType.InvalidParams
      case _: ServerErrorException => ErrorType.ServerError
      case _ => ErrorType.ApplicationError
    }
}
