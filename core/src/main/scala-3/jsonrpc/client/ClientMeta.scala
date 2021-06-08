package jsonrpc.client

import java.lang.reflect.Proxy
import jsonrpc.Client
import jsonrpc.Client.NotTuple
import jsonrpc.client.ClientBindings
import jsonrpc.spi.Codec
import scala.compiletime.summonInline
import scala.reflect.ClassTag
import scala.util.NotGiven

/**
 * JSON-RPC client layer code generation.
 *
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 * @tparam BindingType API method binding type
 */
trait ClientMeta[Node, CodecType <: Codec[Node], Effect[_], Context, BindingType <: Binding[
  Node,
  CodecType,
  Effect,
  Context
]]:
  this: Client[Node, CodecType, Effect, Context, BindingType] =>

  given CanEqual[EmptyTuple, EmptyTuple] = CanEqual.derived
  given CanEqual[EmptyTuple, Tuple] = CanEqual.derived
  given CanEqual[Tuple, EmptyTuple] = CanEqual.derived
  given CanEqual[Tuple, Tuple] = CanEqual.derived

//  client.call[RemoteApi].byName
//  client.call[RemoteApi].byPosition
//
//  client.bind[RemoteApi].byName
//
//  client.bind[RemoteApi].byPosition
//
//  // Callable
//  client.bind("method").call.byName(caseClass)
//
//  client.bind("method").parameters("a", "b", "c")
//  client.bind("method").call.defineNames("a", "b", "c")
//  client.bind("method").call.defineNames("a", "b", "c").byName(tuple)
//
//  // CallableWithoutNames
//  client.bind("method").call.byName(caseClass)
//
//  // CallableWithNames
//  client.bind("method").call.byName(tuple)

  /**
   * Create a specified JSON-RPC API method binding.
   *
   * @param method method name
   * @return method binding
   */
  inline def bind(method: String): BindingType =
    if argumentsByName then
      UnnamedBinding[Node, CodecType, Effect, Context](this, method).asInstanceOf[BindingType]
    else
      PositionalBinding[Node, CodecType, Effect, Context](this, method).asInstanceOf[BindingType]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments method arguments of arbitrary types
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def callByPosition[R](method: String)(arguments: Tuple)(using context: Context): Effect[R] =
    val encodedArguments = Left(encodeArguments(arguments))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by name''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param argumentNames method argument names
   * @param arguments method arguments of arbitrary types
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def callByName[R](method: String)(argumentNames: String*)(arguments: Tuple)(using
    context: Context
  ): Effect[R] =
    val encodedArguments = Right(argumentNames.zip(encodeArguments(arguments)).toMap)
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by name''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments case class containing method arguments of arbitrary types
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def callByName[A <: Product: NotTuple, R](method: String)(arguments: A)(using context: Context): Effect[R] =
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by position''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments method arguments of arbitrary types
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notifyByPosition(method: String)(arguments: Tuple)(using context: Context): Effect[Unit] =
    val encodedArguments = Left(encodeArguments(arguments))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param argumentNames method argument names
   * @param arguments method arguments of arbitrary types
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notifyByName(method: String)(argumentNames: String*)(arguments: Tuple)(using
    context: Context
  ): Effect[Unit] =
    val encodedArguments = Right(argumentNames.zip(encodeArguments(arguments)).toMap)
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments case class containing method arguments of arbitrary types
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notifyByName[A <: Product](method: String)(arguments: A)(using context: Context): Effect[Unit] =
    val argumentsNode = codec.encode(arguments)
    val encodedArguments = Right(codec.decode[Map[String, Node]](argumentsNode))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Create a JSON-RPC API proxy instance with generated method bindings for all valid public methods of the specified API.
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
   * @tparam T API trait type (classes are not supported)
   * @return JSON-RPC API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef]: T =
    // Generate API method bindings
    val methodBindings = ClientBindings.generate[Node, CodecType, Effect, Context, T](codec)

    // Create API proxy instance
    val classTag = summonInline[ClassTag[T]]
    Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array(classTag.runtimeClass),
      (proxy, method, arguments) =>
        // Lookup bindings for the specified method
        methodBindings.get(method.getName).map { clientMethod =>
          // Adjust expected method parameters if it uses context as its last parameter
          val (validArguments, context) =
            if clientMethod.usesContext then
              arguments.dropRight(1).toSeq -> Some(arguments.last).asInstanceOf[Option[Context]]
            else
              arguments.toSeq -> None

          // Encode method arguments
          val argumentNodes = clientMethod.encodeArguments(validArguments)
          val encodedArguments =
            if argumentsByName then
              Right(clientMethod.paramNames.zip(argumentNodes).toMap)
            else
              Left(argumentNodes.toList)

          // Perform the API call
          performCall(method.getName, encodedArguments, context, resultNode => clientMethod.decodeResult)
        }.getOrElse(throw IllegalStateException(s"Method not found: ${method.getName}"))
    ).asInstanceOf[T]

  /**
   * Encode method arguments by position.
   *
   * @param arguments arguments of arbitrary types
   * @return argument nodes
   */
  private[jsonrpc] inline def encodeArguments(arguments: Tuple): List[Node] =
    arguments match
      case EmptyTuple   => List()
      case head *: tail => List(codec.encode(head)) ++ encodeArguments(tail)
