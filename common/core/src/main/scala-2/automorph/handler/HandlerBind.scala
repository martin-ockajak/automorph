package automorph.handler

import automorph.Handler
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Handler method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait HandlerBind[Node, Codec <: MessageCodec[Node], Effect[_], Context] {
  this: Handler[Node, Codec, Effect, Context] =>

  /** This handler type. */
  type ThisHandler = Handler[Node, Codec, Effect, Context]

  /**
   * Creates a copy of this handler with generated RPC bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * Bound API methods are exposed using their actual names.
   *
   * @param api API instance
   * @tparam Api API type (only member methods of this type are exposed)
   * @return RPC request handler with added API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api): ThisHandler =
    macro HandlerBind.basicBindMacro[Node, Codec, Effect, Context, Api]

  /**
   * Creates a copy of this handler with generated RPC bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * Bound API methods are exposed using names resulting from a transcodecion of their actual names via the `aliases` function.
   *
   * @param api API instance
   * @param aliases mapping of method name to its exposed names (empty result causes the method not to be exposed)
   * @tparam Api API type (only member methods of this type are exposed)
   * @return RPC request handler with added API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api, aliases: String => Seq[String]): ThisHandler =
    macro HandlerBind.bindMacro[Node, Codec, Effect, Context, Api]

  def brokenBind[Api <: AnyRef](api: Api): ThisHandler =
    macro HandlerBind.brokenBindMacro[Node, Codec, Effect, Context, Api]
}

case object HandlerBind {

  def brokenBindMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, Codec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[Codec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      automorph.handler.BrokenHandlerBindings
        .generate[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.codec, ${c.prefix}.system, $api)
      ${c.prefix}
    """).asInstanceOf[c.Expr[Handler[Node, Codec, Effect, Context]]]
  }

  def basicBindMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, Codec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[Codec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      ${c.prefix}.copy(bindings = ${c.prefix}.bindings ++ automorph.handler.HandlerBindings
        .generate[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.codec, ${c.prefix}.system, $api)
      )
    """).asInstanceOf[c.Expr[Handler[Node, Codec, Effect, Context]]]
  }

  def bindMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api],
    aliases: c.Expr[String => Seq[String]]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, Codec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[Codec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      ${c.prefix}.copy(bindings = ${c.prefix}.bindings ++ automorph.handler.HandlerBindings
        .generate[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.codec, ${c.prefix}.system, $api)
        .flatMap { case (name, method) =>
          $aliases(name).map(_ -> method)
        }
      )
    """).asInstanceOf[c.Expr[Handler[Node, Codec, Effect, Context]]]
  }
}
