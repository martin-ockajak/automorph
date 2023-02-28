package automorph.handler.meta

import automorph.Handler
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Handler method bindings code generation.
 *
 * @tparam Node
 *   message node type
 * @tparam Codec
 *   message codec plugin type
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   message context type
 */
private[automorph] trait HandlerMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context] {
  this: Handler[Node, Codec, Effect, Context] =>

  /**
   * Creates a copy of this handler with generated RPC bindings for all valid public methods of the specified API
   * instance.
   *
   * An API method is considered valid if it satisfies all of these conditions:
   *   - can be called at runtime
   *   - has no type parameters
   *   - returns the specified effect type
   *   - (if message context type is not EmptyContext.Value) accepts the specified message context type as its last
   *     parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting
   * one the server-supplied ''request context'' is passed to the bound method or the returned context function as its
   * last argument.
   *
   * Bound API methods are exposed as RPC functions using their actual names.
   *
   * @param api
   *   API instance
   * @tparam Api
   *   API type (only member methods of this type are exposed)
   * @return
   *   RPC request handler with specified API bindings
   * @throws java.lang.IllegalArgumentException
   *   if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api): Handler[Node, Codec, Effect, Context] =
    macro HandlerMeta.bindMacro[Node, Codec, Effect, Context, Api]

  /**
   * Creates a copy of this handler with generated RPC bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   *   - can be called at runtime
   *   - has no type parameters
   *   - returns the specified effect type
   *   - (if message context type is not EmptyContext.Value) accepts the specified message context type as its last
   *     parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting
   * one the server-supplied ''request context'' is passed to the bound method or the returned context function as its
   * last argument.
   *
   * Bound API methods are exposed as RPC functions using their names transformed via the `mapName` function.
   *
   * @param api
   *   API instance
   * @param mapName
   *   maps API method name to the exposed RPC function names (empty result causes the method not to be exposed)
   * @tparam Api
   *   API type (only member methods of this type are exposed)
   * @return
   *   RPC request handler with specified API bindings
   * @throws java.lang.IllegalArgumentException
   *   if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](api: Api, mapName: String => Iterable[String]): Handler[Node, Codec, Effect, Context] =
    macro HandlerMeta.bindMapNameMacro[Node, Codec, Effect, Context, Api]
}

object HandlerMeta {

  def bindMacro[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](c: blackbox.Context)(
    api: c.Expr[Api]
  )(implicit
    nodeType: c.WeakTypeTag[Node],
    codecType: c.WeakTypeTag[Codec],
    effectType: c.WeakTypeTag[Effect[?]],
    contextType: c.WeakTypeTag[Context],
    apiType: c.WeakTypeTag[Api],
  ): c.Expr[Handler[Node, Codec, Effect, Context]] = {
    import c.universe.Quasiquote
    Seq(nodeType, codecType, effectType, contextType, apiType)

    val mapName = c.Expr[String => Seq[String]](q"""
      (name: String) => Seq(name)
    """)
    bindMapNameMacro[Node, Codec, Effect, Context, Api](c)(api, mapName)
  }

  def bindMapNameMacro[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](c: blackbox.Context)(
    api: c.Expr[Api], mapName: c.Expr[String => Iterable[String]]
  )(implicit
    nodeType: c.WeakTypeTag[Node],
    codecType: c.WeakTypeTag[Codec],
    effectType: c.WeakTypeTag[Effect[?]],
    contextType: c.WeakTypeTag[Context],
    apiType: c.WeakTypeTag[Api],
  ): c.Expr[Handler[Node, Codec, Effect, Context]] = {
    import c.universe.Quasiquote

    // Handler needs to be assigned to a stable identifier due to macro expansion limitations
    c.Expr[Handler[Node, Codec, Effect, Context]](q"""
      val handler = ${c.prefix}
      val newBindings = automorph.handler.meta.HandlerGenerator
        .bindings[$nodeType, $codecType, $effectType, $contextType, $apiType](
          handler.protocol.codec, $api
        ).flatMap { binding =>
          $mapName(binding.function.name).map(_ -> binding)
        }
      handler.copy(apiBindings = handler.apiBindings ++ newBindings)
    """)
  }
}
