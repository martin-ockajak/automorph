package jsonrpc.handler

import java.beans.IntrospectionException
import jsonrpc.Handler
import jsonrpc.handler.HandlerBindings
import jsonrpc.spi.Codec

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
   * @tparam T API type (only member methods of this type are exposed)
   * @return JSON-RPC server with the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[T <: AnyRef](api: T): Handler[Node, CodecType, Effect, Context] =
    bind(api, name => Seq(name))

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
   * @tparam T API type (only member methods of this type are exposed)
   * @return JSON-RPC server with the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[T <: AnyRef](api: T, exposedNames: String => Seq[String]): Handler[Node, CodecType, Effect, Context] =
    bind(api, Function.unlift(name => Some(exposedNames(name))))

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
   * @param exposedNames define names under which a method is exposed from its actual name (empty result causes the method not to be exposed)
   * @tparam T API type (only member methods of this type are exposed)
   * @return JSON-RPC server with the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[T <: AnyRef](
    api: T,
    exposedNames: PartialFunction[String, Seq[String]]
  ): Handler[Node, CodecType, Effect, Context] = {
    val bindings =
      HandlerBindings.generate[Node, CodecType, Effect, Context, T](codec, backend, api).flatMap {
        case (methodName: String, method: HandlerMethod[Node, Effect, Context]) =>
          exposedNames.applyOrElse(
            methodName,
            (_: String) =>
              throw new IntrospectionException(
                s"Bound API does not contain the specified public method: ${api.getClass.getName}.$methodName"
              )
          ).map(_ -> method)
      }
    copy(methodBindings = methodBindings ++ bindings)
  }
}
