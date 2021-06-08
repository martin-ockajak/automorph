package jsonrpc.client

import jsonrpc.Client
import jsonrpc.client.ClientBindings
import jsonrpc.spi.Codec
import jsonrpc.spi.Codec
import java.lang.reflect.Proxy
import scala.compiletime.summonInline
import scala.reflect.ClassTag

trait ClientMeta[Node, CodecType <: Codec[Node], Effect[_], Context]:
  this: Client[Node, CodecType, Effect, Context] =>

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by name''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
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
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
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
    performNotify(method, encodedArguments, Some(context))

  /**
   * Create a remote JSON-RPC API proxy instance by generating method bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the caller-supplied ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * Bound API method JSON-RPC request arguments are supplied ''by name''.
   *
   * @param api API trait (classes are not supported)
   * @tparam T API type (only member methods of this types are exposed)
   * @return remote JSON-RPC API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef]: T =
    // Generate API method bindings
    val methodBindings = ClientBindings.generate[Node, CodecType, Effect, Context, T](codec)
//    val methodBindings = Map.empty[String, ClientMethod[Node]]

    // Create API proxy instance
    val classTag = summonInline[ClassTag[T]]
    Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array(classTag.runtimeClass),
      (proxy, method, arguments) =>
        // Lookup bindings for the specified method
        methodBindings.get(method.getName).map { clientMethod =>
          // Adjust expected method parameters if it uses context as its last parameter
          val (validArguments, context) = if clientMethod.usesContext then
            (arguments.dropRight(1).toSeq, Some(arguments.last).asInstanceOf[Option[Context]])
          else
            (arguments.toSeq, None)

          // Encode method arguments
          val argumentNodes = clientMethod.encodeArguments(validArguments)
          val encodedArguments = if argumentsByName then
            Right(clientMethod.paramNames.zip(argumentNodes).toMap)
          else
            Left(argumentNodes.toList)

          // Perform the remote API call
          performCall(method.getName, encodedArguments, context, resultNode => clientMethod.decodeResult)
        }.getOrElse(throw IllegalStateException(s"Method not found: ${method.getName}"))
    ).asInstanceOf[T]
