package jsonrpc.client

import jsonrpc.spi.Codec

trait Client[Node, CodecType <: Codec[Node], Effect[_], Context]:

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
  inline def call[A <: Product, R](method: String, arguments: A)(using context: Context): Effect[R]

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
  inline def notify[A <: Product](method: String, arguments: A)(using context: Context): Effect[Unit]

  /**
   * Create a transparent ''proxy instance'' of a remote JSON-RPC API.
   *
   * Invocations of local proxy methods are translated into remote JSON-API calls.
   *
   * @tparam T remote API type
   * @return remote API proxy instance
   */
  inline def bind[T <: AnyRef]: T
