package jsonrpc.handler

import java.beans.IntrospectionException
import jsonrpc.handler.HandlerMacros
import jsonrpc.spi.{Backend, Codec}

trait HandlerMeta[Node, CodecType <: Codec[Node], Effect[_], Context]:
  this: Handler[Node, CodecType, Effect, Context] =>

  /**
   * Create a new JSON-RPC request handler while generating method bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfied all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * API methods are exposed using their actual names.
   *
   * @param api API instance
   * @tparam T API type (only member methods of this types are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](api: T): Handler[Node, CodecType, Effect, Context] =
    bind(api, name => Seq(name))

  /**
   * Create a new JSON-RPC request handler while generating method bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfied all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * API methods are exposed using names resulting from a transformation of their actual names via the `exposedNames` function.
   *
   * @param api API instance
   * @param exposedNames create exposed method names from its actual name (empty result causes the method not to be exposed)
   * @tparam T API type (only member methods of this types are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](
    api: T,
    exposedNames: String => Seq[String]
  ): Handler[Node, CodecType, Effect, Context] =
    bind(api, Function.unlift(name => Some(exposedNames(name))))

  /**
   * Create a new JSON-RPC request handler while generating method bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfied all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * API methods are exposed using names resulting from a transformation of their actual names via the `exposedNames` function.
   *
   * @param api API instance
   * @param exposedNames create exposed method names from its actual name (empty result causes the method not to be exposed)
   * @tparam T API type (only member methods of this types are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](
    api: T,
    exposedNames: PartialFunction[String, Seq[String]]
  ): Handler[Node, CodecType, Effect, Context] =
    val bindings =
      HandlerMacros.bind[Node, CodecType, Effect, Context, T](codec, backend, api).flatMap { (methodName, method) =>
        exposedNames.applyOrElse(
          methodName,
          _ =>
            throw IntrospectionException(
              s"Bound API does not contain the specified public method: ${api.getClass.getName}.$methodName"
            )
        ).map(_ -> method)
      }
    copy(methodBindings = methodBindings ++ bindings)