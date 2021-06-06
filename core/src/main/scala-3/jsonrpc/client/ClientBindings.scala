package jsonrpc.client

import jsonrpc.client.ClientMacros
import jsonrpc.spi.Message.Params
import jsonrpc.spi.Codec
import jsonrpc.util.ValueOps.asRight
import jsonrpc.JsonRpcClient

trait ClientBindings[Node, CodecType <: Codec[Node], Effect[_], Context]:
  this: JsonRpcClient[Node, CodecType, Effect, Context] =>

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by by name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[A <: Product, R](method: String, arguments: A)(using context: Context): Effect[R] =
    performCall(method, encodeArguments(arguments), context, decodeResult[R])

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return nothing
   */
  inline def notify[A <: Product](method: String, arguments: A)(using context: Context): Effect[Unit] =
    performNotify(method, encodeArguments(arguments), context)

  /**
   * Create a transparent ''proxy instance'' of a remote JSON-RPC API.
   *
   * Invocations of local proxy methods are translated into remote JSON-API calls.
   *
   * @tparam T remote API type
   * @return remote API proxy instance
   */
  inline def bind[T <: AnyRef]: T = ClientMacros.bind[Node, CodecType, Effect, Context, T](codec, backend)

  /**
   * Encode request arguments by name.
   *
   * @param arguments request arguments
   * @return encoded request arguments
   */
  inline def encodeArguments[A <: Product](arguments: A): Params[Node] =
    val argumentsNode = codec.encode(arguments)
    codec.decode[Map[String, Node]](argumentsNode).asRight

  /**
   * Create response result decoding function.
   *
   * @tparam R result type
   * @return result decoding function
   */
  inline def decodeResult[R]: Node => R =
    resultNode => codec.decode(resultNode)
