package automorph.handler

import automorph.Handler
import automorph.handler.HandlerBindings
import automorph.spi.{EffectSystem, MessageFormat}

/**
 * Handler method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait HandlerBind[Node, Format <: MessageFormat[Node], Effect[_], Context]:
  this: Handler[Node, Format, Effect, Context] =>

  /** This handler type. */
  type ThisHandler = Handler[Node, Format, Effect, Context]

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
   * @return RPC request handler with added API bindings
   * @throws scala.IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef](api: Api): ThisHandler = bind(api, name => Seq(name))

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
   * Bound API methods are exposed using names resulting from a transformation of their actual names via the `methodAliases` function.
   *
   * @param api API instance
   * @param methodAliases mapping of method name to its exposed names (empty result causes the method not to be exposed)
   * @tparam Api API type (only member methods of this type are exposed)
   * @return RPC request handler with added API bindings
   * @throws IllegalArgumentException if invalid p/ublic methods are found in the API type
   */
  inline def bind[Api <: AnyRef](api: Api, methodAliases: String => Seq[String]): ThisHandler =
    copy(methodBindings =
      methodBindings ++ HandlerBindings.generate[Node, Format, Effect, Context, Api](format, system, api).flatMap {
        (methodName, method) => methodAliases(methodName).map(_ -> method)
      }
    )

  inline def brokenBind[Api <: AnyRef](api: Api): ThisHandler = ???
