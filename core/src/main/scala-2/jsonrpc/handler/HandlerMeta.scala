package jsonrpc.handler

import jsonrpc.Handler
import jsonrpc.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC handler layer code generation.
 *
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[jsonrpc] trait HandlerMeta[Node, CodecType <: Codec[Node], Effect[_], Context] {
  this: Handler[Node, CodecType, Effect, Context] =>

  /**
   * Create a copy of this handler with generated method bindings for all valid public methods of the specified API.
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
   * @return JSON-RPC server with the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api): Handler[Node, CodecType, Effect, Context] =
    macro HandlerMeta.bindDefaultMacro[Node, CodecType, Effect, Context, Api]

  /**
   * Create a copy of this handler with generated method bindings for all valid public methods of the specified API.
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
   * Bound API methods are exposed using names resulting from a transformation of their actual names via the `exposedNames` function.
   *
   * @param api API instance
   * @param exposedNames create exposed method names from its actual name (empty result causes the method not to be exposed)
   * @tparam Api API type (only member methods of this type are exposed)
   * @return JSON-RPC server with the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api, exposedNames: String => Seq[String]): Handler[Node, CodecType, Effect, Context] =
    macro HandlerMeta.bindMacro[Node, CodecType, Effect, Context, Api]
}

case object HandlerMeta {

  def bindMacro[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api],
    exposedNames: c.Expr[String => Seq[String]]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, CodecType, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[CodecType]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Handler[Node, CodecType, Effect, Context]](q"""
      val codec = ${c.prefix}.codec
      val backend = ${c.prefix}.backend
      val bindings = jsonrpc.handler.HandlerBindings
        .bind[$nodeType, $codecType, $effectType, $contextType, $apiType](codec, backend, $api)
        .flatMap { case (methodName, method) =>
          $exposedNames(methodName).map(_ -> method)
      }
      ${c.prefix}.copy(methodBindings = methodBindings ++ bindings)
    """)
  }

  def bindDefaultMacro[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, CodecType, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[CodecType]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Handler[Node, CodecType, Effect, Context]](q"""
      val codec = ${c.prefix}.codec
      val backend = ${c.prefix}.backend
      val bindings = jsonrpc.handler.HandlerBindings
        .bind[$nodeType, $codecType, $effectType, $contextType, $apiType](codec, backend, $api)
      ${c.prefix}.copy(methodBindings = methodBindings ++ bindings)
    """)
  }
}
