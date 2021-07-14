package automorph.handler

import automorph.Handler
import automorph.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Handler method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam ActualCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait HandlerBind[Node, ActualCodec <: Codec[Node], Effect[_], Context] {
  this: Handler[Node, ActualCodec, Effect, Context] =>

  type ThisHandler = Handler[Node, ActualCodec, Effect, Context]

  /**
   * Creates a copy of this handler with generated method bindings for all valid public methods of the specified API.
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
   * @return JSON-RPC server with added API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api): ThisHandler =
    macro HandlerBind.basicBindMacro[Node, ActualCodec, Effect, Context, Api]

  /**
   * Creates a copy of this handler with generated method bindings for all valid public methods of the specified API.
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
   * @return JSON-RPC server with added API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api, mapName: String => Seq[String]): ThisHandler =
    macro HandlerBind.bindMacro[Node, ActualCodec, Effect, Context, Api]
}

case object HandlerBind {

  def basicBindMacro[
    Node: c.WeakTypeTag,
    ActualCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, ActualCodec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[ActualCodec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      ${c.prefix}.copy(methodBindings = ${c.prefix}.methodBindings ++ automorph.handler.HandlerBindings
        .generate[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.codec, ${c.prefix}.backend, $api)
      )
    """).asInstanceOf[c.Expr[Handler[Node, ActualCodec, Effect, Context]]]
  }

  def bindMacro[
    Node: c.WeakTypeTag,
    ActualCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    api: c.Expr[Api],
    mapName: c.Expr[String => Seq[String]]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Handler[Node, ActualCodec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[ActualCodec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Any](q"""
      ${c.prefix}.copy(methodBindings = ${c.prefix}.methodBindings ++ automorph.handler.HandlerBindings
        .generate[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.codec, ${c.prefix}.backend, $api)
        .flatMap { case (methodName, method) =>
          $mapName(methodName).map(_ -> method)
        }
      )
    """).asInstanceOf[c.Expr[Handler[Node, ActualCodec, Effect, Context]]]
  }
}
