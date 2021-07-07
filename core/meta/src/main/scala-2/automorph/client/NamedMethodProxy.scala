package automorph.client

import automorph.spi.{Backend, Codec, Transport}
import automorph.util.CannotEqual
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

case class NamedMethodProxy[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  methodName: String,
  codec: ExactCodec,
  protected val backend: Backend[Effect],
  protected val transport: Transport[Effect, Context],
  protected val errorToException: (Int, String) => Throwable,
  protected val argumentValues: Seq[(String, Any)],
  protected val encodedArguments: Seq[Node]
) extends ClientCore[Node, ExactCodec, Effect, Context] with CannotEqual {

  type PositionalMethod = PositionalMethodProxy[Node, ExactCodec, Effect, Context]
  type NamedMethod = NamedMethodProxy[Node, ExactCodec, Effect, Context]

  /**
   * Create a copy of this method invoker without argument names passing method arguments ''by position''.
   *
   * @return method invoker without argument names passing method arguments ''by position''
   */
  def positional: PositionalMethod = PositionalMethodProxy(
    methodName,
    codec,
    backend,
    transport,
    errorToException,
    argumentValues.map(_._2),
    encodedArguments
  )

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args(): NamedMethod = copy(argumentValues = Seq(), encodedArguments = Seq())

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args[T1](p1: (String, T1)): NamedMethod = macro NamedMethodProxy.argsMacro[NamedMethod, T1]

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args[T1, T2](p1: (String, T1), p2: (String, T2)): NamedMethod = macro NamedMethodProxy.argsMacro[NamedMethod, T1, T2]

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args[T1, T2, T3](p1: (String, T1), p2: (String, T2), p3: (String, T3)): NamedMethod =
    macro NamedMethodProxy.argsMacro[NamedMethod, T1, T2, T3]

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args[T1, T2, T3, T4](p1: (String, T1), p2: (String, T2), p3: (String, T3), p4: (String, T4)): NamedMethod =
    macro NamedMethodProxy.argsMacro[NamedMethod, T1, T2, T3, T4]

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args[T1, T2, T3, T4, T5](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5)
  ): NamedMethod = macro NamedMethodProxy.argsMacro[NamedMethod, T1, T2, T3, T4, T5]

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args[T1, T2, T3, T4, T5, T6](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6)
  ): NamedMethod = macro NamedMethodProxy.argsMacro[NamedMethod, T1, T2, T3, T4, T5, T6]

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args[T1, T2, T3, T4, T5, T6, T7](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6),
    p7: (String, T7)
  ): NamedMethod = macro NamedMethodProxy.argsMacro[NamedMethod, T1, T2, T3, T4, T5, T6, T7]

  /**
   * Send a remote JSON-RPC method ''call'' request with specified result type extracted from the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  def call[R](implicit context: Context): Effect[R] = macro NamedMethodProxy.callMacro[Effect, Context, R]

  /**
   * Send a remote JSON-RPC method ''notification'' request disregarding the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(implicit context: Context): Effect[Unit] =
    notify(methodName, Some(argumentValues.map(_._1)), encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $methodName, Arguments: $argumentValues)"
}

case object NamedMethodProxy {

  def argsMacro[MethodProxyType, T1: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1)
      ))
    """)
  }

  def argsMacro[MethodProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2)
      ))
    """)
  }

  def argsMacro[MethodProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3)
      ))
    """)
  }

  def argsMacro[
    MethodProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4)
      ))
    """)
  }

  def argsMacro[
    MethodProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)],
    p5: c.Expr[(String, T5)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4, $p5),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5)
      ))
    """)
  }

  def argsMacro[
    MethodProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)],
    p5: c.Expr[(String, T5)],
    p6: c.Expr[(String, T6)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4, $p5, $p6),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5),
          ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6)
      ))
    """)
  }

  def argsMacro[
    MethodProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag,
    T7: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)],
    p5: c.Expr[(String, T5)],
    p6: c.Expr[(String, T6)],
    p7: c.Expr[(String, T7)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4, $p5, $p6, $p7),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5),
          ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6),
          ${c.prefix}.codec.encode[${weakTypeOf[T7]}]($p7)
      ))
    """)
  }

  def callMacro[Effect[_], Context, R: c.WeakTypeTag](c: blackbox.Context)(
    context: c.Expr[Context]
  )(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.call(
        ${c.prefix}.methodName,
        Some(${c.prefix}.argumentValues.map(_._1)),
        ${c.prefix}.encodedArguments,
        ${c.prefix}.codec.decode[${weakTypeOf[R]}](_),
        Some($context)
      )
    """)
  }
}
