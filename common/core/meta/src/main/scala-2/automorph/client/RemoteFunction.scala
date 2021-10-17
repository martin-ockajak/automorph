package automorph.client

import automorph.Client
import automorph.spi.MessageCodec
import automorph.util.CannotEqual
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Remote function invocation.
 *
 * @param name function name
 * @param client RPC client
 * @param codec message codec plugin
 * @param arguments argument names and values
 * @param encodedArguments encoded argument values
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class RemoteFunction[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  name: String,
  arguments: Seq[(String, Any)],
  encodedArguments: Seq[Node],
  client: Client[Node, Codec, Effect, Context]
) extends CannotEqual {

  /** Proxy type. */
  type Type = RemoteFunction[Node, Codec, Effect, Context]

  private val codec = client.protocol.codec

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args(): Type = copy(arguments = Seq.empty, encodedArguments = Seq.empty)

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1](p1: (String, T1)): Type =
    macro RemoteFunction.args1Macro[Type, T1]

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2](p1: (String, T1), p2: (String, T2)): Type =
    macro RemoteFunction.args2Macro[Type, T1, T2]

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3](p1: (String, T1), p2: (String, T2), p3: (String, T3)): Type =
    macro RemoteFunction.args3Macro[Type, T1, T2, T3]

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4](p1: (String, T1), p2: (String, T2), p3: (String, T3), p4: (String, T4)): Type =
    macro RemoteFunction.args4Macro[Type, T1, T2, T3, T4]

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4, T5](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5)
  ): Type = macro RemoteFunction.args5Macro[Type, T1, T2, T3, T4, T5]

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4, T5, T6](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6)
  ): Type = macro RemoteFunction.args6Macro[Type, T1, T2, T3, T4, T5, T6]

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args[T1, T2, T3, T4, T5, T6, T7](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6),
    p7: (String, T7)
  ): Type = macro RemoteFunction.args7Macro[Type, T1, T2, T3, T4, T5, T6, T7]

  /**
   * Sends a remote function call request with specified result type extracted from the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  def call[R](implicit context: Context): Effect[R] = macro RemoteFunction.callMacro[Effect, Context, R]

  /**
   * Sends a remote function notification request disregarding the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(implicit context: Context): Effect[Unit] =
    client.notify(name, Some(arguments.map(_._1)), encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $name, Arguments: $arguments)"
}

object RemoteFunction {

  def args1Macro[ProxyType, T1: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        arguments = Seq($p1),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2)
      ))
    """)
  }

  def args2Macro[ProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        arguments = Seq($p1, $p2),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2._2)
      ))
    """)
  }

  def args3Macro[ProxyType, T1: c.WeakTypeTag, T2: c.WeakTypeTag, T3: c.WeakTypeTag](c: blackbox.Context)(
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        arguments = Seq($p1, $p2, $p3),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3._2)
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
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        arguments = Seq($p1, $p2, $p3, $p4),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4._2)
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
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)],
    p5: c.Expr[(String, T5)]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        arguments = Seq($p1, $p2, $p3, $p4, $p5),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5._2)
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
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)],
    p5: c.Expr[(String, T5)],
    p6: c.Expr[(String, T6)]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        arguments = Seq($p1, $p2, $p3, $p4, $p5, $p6),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6._2)
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
    p1: c.Expr[(String, T1)],
    p2: c.Expr[(String, T2)],
    p3: c.Expr[(String, T3)],
    p4: c.Expr[(String, T4)],
    p5: c.Expr[(String, T5)],
    p6: c.Expr[(String, T6)],
    p7: c.Expr[(String, T7)]
  ): c.Expr[ProxyType] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[ProxyType](q"""
      ${c.prefix}.copy(
        arguments = Seq($p1, $p2, $p3, $p4, $p5, $p6, $p7),
        encodedArguments = Seq(
          ${c.prefix}.codec.encode[${weakTypeOf[T1]}]($p1._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T2]}]($p2._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T3]}]($p3._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T4]}]($p4._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T5]}]($p5._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T6]}]($p6._2),
          ${c.prefix}.codec.encode[${weakTypeOf[T7]}]($p7._2)
      ))
    """)
  }

  def callMacro[Effect[_], Context, R: c.WeakTypeTag](c: blackbox.Context)(
    context: c.Expr[Context]
  )(implicit resultType: c.WeakTypeTag[Effect[R]]): c.Expr[Effect[R]] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[Effect[R]](q"""
      ${c.prefix}.client.call(
        ${c.prefix}.name,
        ${c.prefix}.arguments.map(_._1),
        ${c.prefix}.encodedArguments,
        ${c.prefix}.codec.decode[${weakTypeOf[R]}](_),
        Some($context)
      )
    """)
  }
}
