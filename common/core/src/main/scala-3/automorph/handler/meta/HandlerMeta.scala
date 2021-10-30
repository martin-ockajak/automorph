package automorph.handler.meta

import automorph.Handler
import automorph.spi.{EffectSystem, MessageCodec}

/**
 * Handler method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
private[automorph] trait HandlerMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context]:
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
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef](api: Api): ThisHandler = bind(api, name => Seq(name))

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
   * Bound API methods are exposed using names resulting from a transformation of their actual names via the `mapNames` function.
   *
   * @param api API instance
   * @param mapNames maps API method name to its exposed RPC function names (empty result causes the method not to be exposed)
   * @tparam Api API type (only member methods of this type are exposed)
   * @return RPC request handler with specified API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef](api: Api, mapNames: String => Iterable[String]): ThisHandler =
    val newBindings =
      bindings ++ HandlerGenerator.bindings[Node, Codec, Effect, Context, Api](protocol.codec, system, api).flatMap { binding =>
        mapNames(binding.function.name).map(_ -> binding)
      }
    copy(bindings = newBindings)

  inline def brokenBind[Api <: AnyRef](api: Api): ThisHandler = ???