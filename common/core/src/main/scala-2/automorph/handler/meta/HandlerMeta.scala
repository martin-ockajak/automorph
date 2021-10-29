package automorph.handler.meta

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
 * @tparam Context message context type
 */
private[automorph] trait HandlerMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context] {
  this: Handler[Node, Codec, Effect, Context] =>

  /** This handler type. */
  type ThisHandler = Handler[Node, Codec, Effect, Context]

  /**
   * Creates a copy of this handler with generated RPC bindings for all valid public methods of the specified API instance.
   *
   * An API method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if message context type is not EmptyContext.Value) accepts the specified message context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * Bound API methods are exposed using their actual names.
   *
   * @param api API instance
   * @tparam Api API type (only member methods of this type are exposed)
   * @return RPC request handler with specified API bindings
   * @throws java.lang.IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api): ThisHandler =
    macro HandlerMeta.basicBindMacro[Node, Codec, Effect, Context, Api]

  /**
   * Creates a copy of this handler with generated RPC bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if message context type is not EmptyContext.Value) accepts the specified message context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * Bound API methods are exposed using names resulting from a transformation of their actual names via the `mapNames` function.
   *
   * @param api API instance
   * @param mapNames maps API method name to its exposed RPC function names (empty result causes the method not to be exposed)
   * @tparam Api API type (only member methods of this type are exposed)
   * @return RPC request handler with specified API bindings
   * @throws java.lang.IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api, mapNames: String => Iterable[String]): ThisHandler =
    macro HandlerMeta.bindMacro[Node, Codec, Effect, Context, Api]
//
//  def brokenBind[Api <: AnyRef](api: Api): ThisHandler =
//    macro HandlerMeta.brokenBindMacro[Node, Codec, Effect, Context, Api]
}

object HandlerMeta {

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
      automorph.handler.meta.BrokenHandlerGenerator
        .bindings[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.protocol.codec, ${c.prefix}.system, $api)
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
      val newBindings = ${c.prefix}.bindings ++ automorph.handler.meta.HandlerGenerator
        .bindings[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.protocol.codec, ${c.prefix}.system, $api)
        .map { binding =>
          binding.function.name -> binding
        }
      ${c.prefix}.copy(bindings = newBindings)
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
    mapNames: c.Expr[String => Iterable[String]]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, Codec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[Codec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      val newBindings = ${c.prefix}.bindings ++ automorph.handler.HandlerGenerator
        .bindings[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.protocol.codec, ${c.prefix}.system, $api)
        .flatMap { binding =>
          $mapNames(binding.function.name).map(_ -> binding)
        }
      ${c.prefix}.copy(bindings = newBindings)
    """).asInstanceOf[c.Expr[Handler[Node, Codec, Effect, Context]]]
  }
}
