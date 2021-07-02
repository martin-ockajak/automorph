package jsonrpc.client

import jsonrpc.Client
import jsonrpc.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC client layer code generation.
 *
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[jsonrpc] trait ClientMeta[Node, ExactCodec <: Codec[Node], Effect[_], Context] {
  this: Client[Node, ExactCodec, Effect, Context] =>

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[R](method: String)(implicit context: Context): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, R](method: String, p1: T1)(implicit context: Context): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, T1, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, R](method: String, p1: T1, p2: T2)(implicit context: Context): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, T1, T2, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, R](method: String, p1: T1, p2: T2, p3: T3)(implicit context: Context): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, T1, T2, T3, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, R]
  (method: String, p1: T1, p2: T2, p3: T3, p4: T4)(implicit context: Context): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, T1, T2, T3, T4, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, T5, R]
  (method: String, p1: T1, p2: T2, p3: T3, p4: T4, p5: T5)(implicit context: Context): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, T1, T2, T3, T4, T5, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, T5, T6, R](method: String, p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6)(
    implicit context: Context
  ): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, T1, T2, T3, T4, T5, T6, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByPosition[T1, T2, T3, T4, T5, T6, T7, R]
  (method: String, p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7)(
    implicit context: Context
  ): Effect[R] =
    macro ClientMeta.callByPositionMacro[Effect, Context, T1, T2, T3, T4, T5, T6, T7, R]

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent named method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def callByName[T1, R](method: String, p1: (String , T1))(implicit context: Context): Effect[R] =
    macro ClientMeta.callByNameMacro[Effect, Context, T1, R]

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition(method: String)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context]


  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1](method: String, p1: T1)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context, T1]


  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2](method: String, p1: T1, p2: T2)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context, T1, T2]


  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3](method: String, p1: T1, p2: T2, p3: T3)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context, T1, T2, T3]


  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4]
  (method: String, p1: T1, p2: T2, p3: T3, p4: T4)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context, T1, T2, T3, T4]


  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4, T5]
  (method: String, p1: T1, p2: T2, p3: T3, p4: T4, p5: T5)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context, T1, T2, T3, T4, T5]


  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4, T5, T6]
  (method: String, p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context, T1, T2, T3, T4, T5, T6]


  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the arguments ''by name''.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method arguments and type parameters 'T1', 'T2' ... 'TN' their respective types.
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @return nothing
   */
  def notifyByPosition[T1, T2, T3, T4, T5, T6, T7]
  (method: String, p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7)(implicit context: Context): Effect[Unit] =
    macro ClientMeta.notifyByPositionMacro[Effect, Context, T1, T2, T3, T4, T5, T6, T7]


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
  def bind[Api <: AnyRef]: Api = macro ClientMeta.bindMacro[Node, ExactCodec, Effect, Context, Api]
}

object ClientMeta {

  def bindMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[ExactCodec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Api](q"""
      // Generate API method bindings
      val codec = ${c.prefix}.codec
      val methodBindings = jsonrpc.client.ClientBindings.generate[$nodeType, $codecType, $effectType, $contextType, $apiType](codec)

      // Create API proxy instance
      java.lang.reflect.Proxy.newProxyInstance(
        getClass.getClassLoader,
        Array(classOf[$apiType]),
        (_, method, arguments) =>
          // Lookup bindings for the specified method
          methodBindings.get(method.getName).map { clientMethod =>
            // Adjust expected method parameters if it uses context as its last parameter
            val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
            val (argumentValues, context) =
              if (clientMethod.usesContext && callArguments.nonEmpty) {
                callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[$contextType])
              } else {
                callArguments.toSeq -> None
              }

            // Encode method arguments
            val argumentNodes = clientMethod.encodeArguments(argumentValues)
            val encodedArguments =
              if (${c.prefix}.argumentsByName) {
                Right(clientMethod.paramNames.zip(argumentNodes).toMap)
              } else {
                Left(argumentNodes.toList)
              }

            // Perform the API call
            ${c.prefix}.performCall(method.getName, encodedArguments, context, resultNode => clientMethod.decodeResult(resultNode))
          }.getOrElse(throw new UnsupportedOperationException("Invalid method: " + method.getName))
      ).asInstanceOf[$apiType]
    """)
  }

  def callByPositionMacro[Effect[_], Context, R: c.WeakTypeTag](c: blackbox.Context)(
    method: c.Expr[String]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List()), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByPositionMacro[Effect[_], Context, T1: c.WeakTypeTag, R: c.WeakTypeTag](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    R: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2)
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    R: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3)
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    R: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4)
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    R: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
        ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5)
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag,
    R: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5],
    p6: c.Expr[T6]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
        ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5),
        ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6)
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag,
    T7: c.WeakTypeTag,
    R: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5],
    p6: c.Expr[T6],
    p7: c.Expr[T7]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
        ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5),
        ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6),
        ${c.prefix}.codec.encode[${weakTypeOf[T7]}]($p7)
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def callByNameMacro[Effect[_], Context, T1: c.WeakTypeTag, R: c.WeakTypeTag](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[(String, T1)]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.performCall($method, Right(Map(
        $p1._1 -> ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2)
      )), Some($context), resultNode => ${c.prefix}.codec.decode[${weakTypeOf[R]}](resultNode))
    """)
  }

  def notifyByPositionMacro[Effect[_], Context](c: blackbox.Context)(
    method: c.Expr[String]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.Quasiquote

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List()), Some($context))
    """)
  }

  def notifyByPositionMacro[Effect[_], Context, T1: c.WeakTypeTag](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
      )), Some($context))
    """)
  }

  def notifyByPositionMacro[Effect[_], Context, T1: c.WeakTypeTag, T2: c.WeakTypeTag](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2)
      )), Some($context))
    """)
  }

  def notifyByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3)
      )), Some($context))
    """)
  }

  def notifyByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4)
      )), Some($context))
    """)
  }

  def notifyByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
        ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5)
      )), Some($context))
    """)
  }

  def notifyByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5],
    p6: c.Expr[T6]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
        ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5),
        ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6)
      )), Some($context))
    """)
  }

  def notifyByPositionMacro[
    Effect[_],
    Context,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag,
    T7: c.WeakTypeTag
  ](c: blackbox.Context)(
    method: c.Expr[String],
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5],
    p6: c.Expr[T6],
    p7: c.Expr[T7]
  )(context: c.Expr[Context])(implicit resultType: c.WeakTypeTag[Effect[Unit]]): c.Expr[Effect[Unit]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Effect[Unit]](q"""
      ${c.prefix}.performNotify($method, Left(List(
        ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
        ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
        ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
        ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
        ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5),
        ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6),
        ${c.prefix}.codec.encode[${weakTypeOf[T7]}]($p7)
      )), Some($context))
    """)
  }
}
