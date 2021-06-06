package jsonrpc.client

import jsonrpc.client.ClientMacros
import jsonrpc.spi.Message.Params
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.spi.Codec

trait ClientBindings[Node, CodecType <: Codec[Node], Effect[_], Context]
  extends Client[Node, CodecType, Effect, Context]:

  override inline def call[A <: Product, R](method: String, arguments: A)(using context: Context): Effect[R] =
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performCall(method, encodedArguments, context, resultNode => codec.decode(resultNode))

  override inline def notify[A <: Product](method: String, arguments: A)(using context: Context): Effect[Unit] =
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performNotify(method, encodedArguments, context)

  override inline def bind[T <: AnyRef]: T = ClientMacros.bind[Node, CodecType, Effect, Context, T](codec, backend)

  /**
   * Perform a method call using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param arguments method arguments
   * @param context request context
   * @param decodeResult result decoding function
   * @tparam R result type
   * @return result value
   */
  protected def performCall[R](
    method: String,
    arguments: Params[Node],
    context: Context,
    decodeResult: Node => R
  ): Effect[R]

  /**
   * Perform a method notification using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param arguments method arguments
   * @param context request context
   * @tparam R result type
   * @return nothing
   */
  protected def performNotify(methodName: String, arguments: Params[Node], context: Context): Effect[Unit]
