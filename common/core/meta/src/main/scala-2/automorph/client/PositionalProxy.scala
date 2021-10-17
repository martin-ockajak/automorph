package automorph.client

import automorph.spi.MessageCodec
import automorph.util.CannotEqual
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

final case class PositionalProxy[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  functionName: String,
  core: ClientCore[Node, Codec, Effect, Context],
  codec: Codec,
  argumentValues: Seq[Any],
  encodedArguments: Seq[Node]
) extends CannotEqual {

  /** Proxy type. */
  type Type = PositionalProxy[Node, Codec, Effect, Context]

  /**
   * Creates a copy of this function proxy passing function arguments by name with specified argument names.
   *
   * @param argumentNames function argument names
   * @return function proxy
   */
  def named(argumentNames: String*): NamedProxy[Node, Codec, Effect, Context] = NamedProxy(
    functionName,
    core,
    codec,
    argumentNames.zip(argumentValues),
    Option.when(argumentNames.size == argumentValues.size)(encodedArguments).getOrElse {
      throw new IllegalArgumentException(s"Supplied ${argumentNames.size} argument names instead of ${argumentValues.size} required")
    }
  )

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args(): Type = copy(argumentValues = Seq.empty, encodedArguments = Seq.empty)

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1](p1: T1): Type =
    macro PositionalProxy.args1Macro[Type, T1]

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2](p1: T1, p2: T2): Type =
    macro PositionalProxy.args2Macro[Type, T1, T2]

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3](p1: T1, p2: T2, p3: T3): Type =
    macro PositionalProxy.args3Macro[Type, T1, T2, T3]

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4](p1: T1, p2: T2, p3: T3, p4: T4): Type =
    macro PositionalProxy.args4Macro[Type, T1, T2, T3, T4]

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4, T5](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5): Type =
    macro PositionalProxy.args5Macro[Type, T1, T2, T3, T4, T5]

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4, T5, T6](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6): Type =
    macro PositionalProxy.args6Macro[Type, T1, T2, T3, T4, T5, T6]

  /**
   * Creates a copy of this function proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4, T5, T6, T7](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7): Type =
    macro PositionalProxy.args7Macro[Type, T1, T2, T3, T4, T5, T6, T7]

  /**
   * Sends a remote function call request with specified result type extracted from the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  def call[R](implicit context: Context): Effect[R] = macro PositionalProxy.callMacro[Effect, Context, R]

  /**
   * Sends a remote function notification request disregarding the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(implicit context: Context): Effect[Unit] =
    core.notify(functionName, None, encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $functionName, Arguments: $argumentValues)"
}

object PositionalProxy {

  def args1Macro[ProxyType, T1: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[T1]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1)
      ))
    """)
  }

  def args2Macro[ProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2)
      ))
    """)
  }

  def args3Macro[ProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3)
      ))
    """)
  }

  def args4Macro[
    ProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4)
      ))
    """)
  }

  def args5Macro[
    ProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4, $p5), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5)
      ))
    """)
  }

  def args6Macro[
    ProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5],
    p6: c.Expr[T6]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4, $p5, $p6), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5),
          ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6)
      ))
    """)
  }

  def args7Macro[
    ProxyType,
    T1: c.WeakTypeTag,
    T2: c.WeakTypeTag,
    T3: c.WeakTypeTag,
    T4: c.WeakTypeTag,
    T5: c.WeakTypeTag,
    T6: c.WeakTypeTag,
    T7: c.WeakTypeTag
  ](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5],
    p6: c.Expr[T6],
    p7: c.Expr[T7]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4, $p5, $p6, $p7), encodedArguments = Seq(
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
      ${c.prefix}.core.call(
        ${c.prefix}.functionName,
        None,
        ${c.prefix}.encodedArguments,
        ${c.prefix}.codec.decode[${weakTypeOf[R]}](_),
        Some($context)
      )
    """)
  }
}
