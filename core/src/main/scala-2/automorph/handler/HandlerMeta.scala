package automorph.handler

import automorph.Handler
import automorph.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC handler layer code generation.
 *
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait HandlerMeta[Node, ExactCodec <: Codec[Node], Effect[_], Context] {
  this: Handler[Node, ExactCodec, Effect, Context] =>

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
  def bind[Api <: AnyRef](api: Api): Handler[Node, ExactCodec, Effect, Context] =
    macro HandlerMeta.bindDefaultMacro[Node, ExactCodec, Effect, Context, Api]

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
   * Bound API methods are exposed using names resulting from a transformation of their actual names via the `mapName` function.
   *
   * @param api API instance
   * @param mapName mapping of method name to its exposed names (empty result causes the method not to be exposed)
   * @tparam Api API type (only member methods of this type are exposed)
   * @return JSON-RPC server with the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api, mapName: String => Seq[String]): Handler[Node, ExactCodec, Effect, Context] =
    macro HandlerMeta.bindMacro[Node, ExactCodec, Effect, Context, Api]
}

case object HandlerMeta {

  def bindDefaultMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, ExactCodec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[ExactCodec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      ${c.prefix}.copy(methodBindings = ${c.prefix}.methodBindings ++ automorph.handler.HandlerBindings
        .generate[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.codec, ${c.prefix}.backend, $api)
      )
    """).asInstanceOf[c.Expr[Handler[Node, ExactCodec, Effect, Context]]]
  }

  def bindMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api],
    mapName: c.Expr[String => Seq[String]]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, ExactCodec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[ExactCodec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      ${c.prefix}.copy(methodBindings = ${c.prefix}.methodBindings ++ automorph.handler.HandlerBindings
        .generate[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.codec, ${c.prefix}.backend, $api)
        .flatMap { case (methodName, method) =>
          $mapName(methodName).map(_ -> method)
        }
      )
    """).asInstanceOf[c.Expr[Handler[Node, ExactCodec, Effect, Context]]]
  }
}
