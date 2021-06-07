package jsonrpc.client

import jsonrpc.Client
import jsonrpc.client.ClientMacros
import jsonrpc.spi.Message.Params
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.spi.Codec
import java.lang.reflect.Proxy
import scala.compiletime.summonInline
import scala.reflect.ClassTag

trait ClientMeta[Node, CodecType <: Codec[Node], Effect[_], Context]:
  this: Client[Node, CodecType, Effect, Context] =>

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
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performCall(method, encodedArguments, context, resultNode => codec.decode(resultNode))

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
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performNotify(method, encodedArguments, context)

  inline def bind[T <: AnyRef]: T =
    val classTag = summonInline[ClassTag[T]]
    Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array(classTag.runtimeClass),
      (proxy, method, methodArgs) =>
        println(method.getName)
        ()
    ).asInstanceOf[T]
//    ClientMacros.bind[Node, CodecType, Effect, Context, T](codec, backend)
