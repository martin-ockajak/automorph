package jsonrpc.client

import jsonrpc.client.ClientMacros
import jsonrpc.spi.Message.Params
import jsonrpc.spi.Codec
import jsonrpc.JsonRpcClient

trait ClientBindings[Node, CodecType <: Codec[Node], Effect[_], Context]
  extends Client[Node, CodecType, Effect, Context]:
  this: JsonRpcClient[Node, CodecType, Effect, Context] =>

  override inline def call[A <: Product, R](method: String, arguments: A)(using context: Context): Effect[R] =
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performCall(method, encodedArguments, context, resultNode => codec.decode(resultNode))

  override inline def notify[A <: Product](method: String, arguments: A)(using context: Context): Effect[Unit] =
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performNotify(method, encodedArguments, context)

  override inline def bind[T <: AnyRef]: T = ClientMacros.bind[Node, CodecType, Effect, Context, T](codec, backend)
