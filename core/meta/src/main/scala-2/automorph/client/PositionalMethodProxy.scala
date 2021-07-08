package automorph.client

import automorph.spi.{Backend, Codec, Transport}
import automorph.util.CannotEqual
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

case class PositionalMethodProxy[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  methodName: String,
  codec: ExactCodec,
  protected val backend: Backend[Effect],
  protected val transport: Transport[Effect, Context],
  protected val errorToException: (Int, String) => Throwable,
  protected val argumentValues: Seq[Any],
  protected val encodedArguments: Seq[Node]
) extends ClientCore[Node, ExactCodec, Effect, Context] with CannotEqual {

  type PositionalMethod = PositionalMethodProxy[Node, ExactCodec, Effect, Context]
  type NamedMethod = NamedMethodProxy[Node, ExactCodec, Effect, Context]

  /**
   * Creates a copy of this method proxy passing method arguments ''by name'' with specified argument names.
   *
   * @param argumentNames method argument names
   * @return method proxy
   */
  def named(argumentNames: String*): NamedMethod = NamedMethodProxy(
    methodName,
    codec,
    backend,
    transport,
    errorToException,
    argumentNames.zip(argumentValues),
    Option.when(argumentNames.size == argumentValues.size)(encodedArguments).getOrElse {
      throw new IllegalArgumentException(s"Supplied ${argumentNames.size} argument names instead of ${argumentValues.size} required")
    }
  )

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args(): PositionalMethod = copy(argumentValues = Seq(), encodedArguments = Seq())

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args[T1](p1: T1): PositionalMethod = macro PositionalMethodProxy.argsMacro[PositionalMethod, T1]

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args[T1, T2](p1: T1, p2: T2): PositionalMethod = macro PositionalMethodProxy.argsMacro[PositionalMethod, T1, T2]

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args[T1, T2, T3](p1: T1, p2: T2, p3: T3): PositionalMethod = macro PositionalMethodProxy.argsMacro[PositionalMethod, T1, T2, T3]

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args[T1, T2, T3, T4](p1: T1, p2: T2, p3: T3, p4: T4): PositionalMethod =
    macro PositionalMethodProxy.argsMacro[PositionalMethod, T1, T2, T3, T4]

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args[T1, T2, T3, T4, T5](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5): PositionalMethod =
    macro PositionalMethodProxy.argsMacro[PositionalMethod, T1, T2, T3, T4, T5]

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args[T1, T2, T3, T4, T5, T6](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6): PositionalMethod =
    macro PositionalMethodProxy.argsMacro[PositionalMethod, T1, T2, T3, T4, T5, T6]

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args[T1, T2, T3, T4, T5, T6, T7](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7): PositionalMethod =
    macro PositionalMethodProxy.argsMacro[PositionalMethod, T1, T2, T3, T4, T5, T6, T7]

  /**
   * Sends a remote JSON-RPC method ''call'' request with specified result type extracted from the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  def call[R](implicit context: Context): Effect[R] = macro NamedMethodProxy.callMacro[Effect, Context, R]

  /**
   * Sends a remote JSON-RPC method ''notification'' request disregarding the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(implicit context: Context): Effect[Unit] =
    notify(methodName, None, encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $methodName, Arguments: $argumentValues)"
}

case object PositionalMethodProxy {

  def argsMacro[MethodProxyType, T1: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[T1]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1)
      ))
    """)
  }

  def argsMacro[MethodProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2), encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2)
      ))
    """)
  }

  def argsMacro[MethodProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3), encodedArguments = Seq(
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
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentValues = Seq($p1, $p2, $p3, $p4), encodedArguments = Seq(
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
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
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

  def argsMacro[
    MethodProxyType,
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
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
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
    p1: c.Expr[T1],
    p2: c.Expr[T2],
    p3: c.Expr[T3],
    p4: c.Expr[T4],
    p5: c.Expr[T5],
    p6: c.Expr[T6],
    p7: c.Expr[T7]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
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

  def namedArgsMacro[MethodProxyType, T1: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentNames = Some(Seq($p1._1)),
        argumentValues = Seq($p1._2),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1)
      ))
    """)
  }

  def namedArgsMacro[MethodProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentNames = Some(Seq($p1._1, $p2._1)),
        argumentValues = Seq($p1._2, $p2._2),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2)
      ))
    """)
  }

  def namedArgsMacro[MethodProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)]
  ): c.Expr[MethodProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[MethodProxyType](q"""
      ${c.prefix}.copy(
        argumentNames = Some(Seq($p1._1, $p2._1, $p3._1)),
        argumentValues = Seq($p1._2, $p2._2, $p3._2),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3)
      ))
    """)
  }

  def namedArgsMacro[
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
        argumentNames = Some(Seq($p1._1, $p2._1, $p3._1, $p4._1)),
        argumentValues = Seq($p1._2, $p2._2, $p3._2, $p4._2),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4)
      ))
    """)
  }

  def namedArgsMacro[
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
        argumentNames = Some(Seq($p1._1, $p2._1, $p3._1, $p4._1, $p5._1)),
        argumentValues = Seq($p1._2, $p2._2, $p3._2, $p4._2, $p5._2),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5)
      ))
    """)
  }

  def namedArgsMacro[
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
        argumentNames = Some(Seq($p1._1, $p2._1, $p3._1, $p4._1, $p5._1, $p6._1)),
        argumentValues = Seq($p1._2, $p2._2, $p3._2, $p4._2, $p5._2, $p6._2),
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

  def callMacro[Effect[_], Context, R: c.WeakTypeTag](c: blackbox.Context)(
    context: c.Expr[Context]
  )(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.call(
        ${c.prefix}.methodName,
        None,
        ${c.prefix}.encodedArguments,
        ${c.prefix}.codec.decode[${weakTypeOf[R]}](_),
        Some($context)
      )
    """)
  }
}
