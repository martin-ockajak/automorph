package jsonrpc.client

import java.lang.reflect.Proxy
import jsonrpc.Client
import jsonrpc.client.ClientBindings
import jsonrpc.spi.Codec
import scala.reflect.ClassTag

/**
 * JSON-RPC client layer code generation.
 *
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[jsonrpc] trait ClientMeta[Node, CodecType <: Codec[Node], Effect[_], Context] {
  this: Client[Node, CodecType, Effect, Context] =>

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[R](method: String)()(implicit context: Context): Effect[R] = {
    val encodedArguments = Left(List())
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, R](method: String)(p1: T1)(implicit context: Context): Effect[R] = {
    val encodedArguments = Left(List(
      codec.encode(p1)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, R](method: String)(p1: T1, p2: T2)(implicit context: Context): Effect[R] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, R](method: String)(p1: T1, p2: T2, p3: T3)(implicit context: Context): Effect[R] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, R](method: String)(p1: T1, p2: T2, p3: T3, p4: T4)(implicit
    context: Context
  ): Effect[R] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, T5, R](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5)(implicit
    context: Context
  ): Effect[R] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, T5, T6, R](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6)(
    implicit context: Context
  ): Effect[R] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, T5, T6, T7, R](method: String)(
    p1: T1,
    p2: T2,
    p3: T3,
    p4: T4,
    p5: T5,
    p6: T6,
    p7: T7
  )(
    implicit context: Context
  ): Effect[R] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6),
      codec.encode(p7)
    ))
    performCall(method, encodedArguments, Some(context), resultNode => codec.decode[R](resultNode))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition(method: String)()(implicit context: Context): Effect[Unit] = {
    val encodedArguments = Left(List())
    performNotify(method, encodedArguments, Some(context))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1](method: String)(p1: T1)(implicit context: Context): Effect[Unit] = {
    val encodedArguments = Left(List(
      codec.encode(p1)
    ))
    performNotify(method, encodedArguments, Some(context))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2](method: String)(p1: T1, p2: T2)(implicit context: Context): Effect[Unit] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2)
    ))
    performNotify(method, encodedArguments, Some(context))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3](method: String)(p1: T1, p2: T2, p3: T3)(implicit
    context: Context
  ): Effect[Unit] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3)
    ))
    performNotify(method, encodedArguments, Some(context))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4](method: String)(p1: T1, p2: T2, p3: T3, p4: T4)(implicit
    context: Context
  ): Effect[Unit] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4)
    ))
    performNotify(method, encodedArguments, Some(context))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4, T5](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5)(implicit
    context: Context
  ): Effect[Unit] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5)
    ))
    performNotify(method, encodedArguments, Some(context))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4, T5, T6](method: String)(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6)(
    implicit context: Context
  ): Effect[Unit] = {
    val encodedArguments = Left(List(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6)
    ))
    performNotify(method, encodedArguments, Some(context))
  }

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent invoked method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4, T5, T6, T7](method: String)(
    p1: T1,
    p2: T2,
    p3: T3,
    p4: T4,
    p5: T5,
    p6: T6,
    p7: T7
  )(implicit context: Context): Effect[Unit] = {
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
  }

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
   * @tparam Api API trait type (classes are not supported)
   * @return JSON-RPC API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef: ClassTag]: Api = {
    // Generate API method bindings
    val methodBindings = ClientBindings.generate[Node, CodecType, Effect, Context, Api](codec)

    // Create API proxy instance
    val classTag = implicitly[ClassTag[Api]]
    Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array(classTag.runtimeClass),
      (_, method, arguments) =>
        // Lookup bindings for the specified method
        methodBindings.get(method.getName).map { clientMethod =>
          // Adjust expected method parameters if it uses context as its last parameter
          val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
          val (argumentValues, context) =
            if (clientMethod.usesContext && callArguments.nonEmpty) {
              callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[Context])
            } else {
              callArguments.toSeq -> None
            }

          // Encode method arguments
          val argumentNodes = clientMethod.encodeArguments(argumentValues)
          val encodedArguments =
            if (argumentsByName) {
              Right(clientMethod.paramNames.zip(argumentNodes).toMap)
            } else {
              Left(argumentNodes.toList)
            }

          // Perform the API call
          performCall(method.getName, encodedArguments, context, resultNode => clientMethod.decodeResult(resultNode))
        }.getOrElse(throw new IllegalStateException(s"Method not found: ${method.getName}"))
    ).asInstanceOf[Api]
  }
}
