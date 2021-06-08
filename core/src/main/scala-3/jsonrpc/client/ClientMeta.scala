package jsonrpc.client

import jsonrpc.Client
import jsonrpc.client.ClientBindings
import jsonrpc.spi.Codec
import jsonrpc.spi.Codec
import java.lang.reflect.Proxy
import scala.compiletime.summonInline
import scala.reflect.ClassTag

/**
 * JSON-RPC client layer code generation.
 *
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
trait ClientMeta[Node, CodecType <: Codec[Node], Effect[_], Context]:
  this: Client[Node, CodecType, Effect, Context] =>

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[R](method: String)()(using context: Context): Effect[R] =
    val encodedArguments = Left(List())
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[T1, R](method: String)(p1: T1)(using context: Context): Effect[R] =
    val encodedArguments = Left(List(
      codec.encode(p1)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[T1, T2, R](method: String)(p1: T1, p2: T2)(using context: Context): Effect[R] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[T1, T2, T3, R](method: String)(p1: T1, p2: T2, p3: T3)(using context: Context): Effect[R] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[T1, T2, T3, T4, R](method: String)(p1: T1, p2: T2, p3: T3, p4: T4)(using
    context: Context
  ): Effect[R] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[T1, T2, T3, T4, T5, R](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5)(using
    context: Context
  ): Effect[R] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[T1, T2, T3, T4, T5, T6, R](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6)(using
    context: Context
  ): Effect[R] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[T1, T2, T3, T4, T5, T6, T7, R](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7)(
    using context: Context
  ): Effect[R] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6),
      codec.encode(p7)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode(resultNode))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify(method: String)()(using context: Context): Effect[Unit] =
    val encodedArguments = Left(List())
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify[T1](method: String)(p1: T1)(using context: Context): Effect[Unit] =
    val encodedArguments = Left(List(
      codec.encode(p1)
    ))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify[T1, T2](method: String)(p1: T1, p2: T2)(using context: Context): Effect[Unit] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2)
    ))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify[T1, T2, T3](method: String)(p1: T1, p2: T2, p3: T3)(using context: Context): Effect[Unit] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3)
    ))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify[T1, T2, T3, T4](method: String)(p1: T1, p2: T2, p3: T3, p4: T4)(using
    context: Context
  ): Effect[Unit] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4)
    ))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify[T1, T2, T3, T4, T5](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5)(using
    context: Context
  ): Effect[Unit] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5)
    ))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify[T1, T2, T3, T4, T5, T6](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6)(using
    context: Context
  ): Effect[Unit] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6)
    ))
    performNotify(method, encodedArguments, Some(context))

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'aN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  inline def notify[T1, T2, T3, T4, T5, T6, T7](method: String)(
    p1: T1,
    p2: T2,
    p3: T3,
    p4: T4,
    p5: T5,
    p6: T6,
    p7: T7
  )(using context: Context): Effect[Unit] =
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6),
      codec.encode(p7)
    ))
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
          val (validArguments, context) =
            if clientMethod.usesContext then
              (arguments.dropRight(1).toSeq, Some(arguments.last).asInstanceOf[Option[Context]]
            )
            else
              (arguments.toSeq, None
            )

          // Encode method arguments
          val argumentNodes = clientMethod.encodeArguments(validArguments)
          val encodedArguments =
            if argumentsByName then
              Right(clientMethod.paramNames.zip(argumentNodes).toMap)
            else
              Left(argumentNodes.toList)

          // Perform the remote API call
          performCall(method.getName, encodedArguments, context, resultNode => clientMethod.decodeResult)
        }.getOrElse(throw IllegalStateException(s"Method not found: ${method.getName}"))
    ).asInstanceOf[T]
