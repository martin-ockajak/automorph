package jsonrpc.protocol

private[jsonrpc] object ErrorType:

  /** JSON-RPC error types with codes. */
  enum ErrorType(val code: Int):

    case ParseError extends ErrorType(-32700)
    case InvalidRequest extends ErrorType(-32600)
    case MethodNotFound extends ErrorType(-32601)
    case InvalidParams extends ErrorType(-32602)
    case InternalError extends ErrorType(-32603)
    case IOError extends ErrorType(-32000)
    case ApplicationError extends ErrorType(0)
