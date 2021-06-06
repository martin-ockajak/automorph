package jsonrpc.handler

import java.beans.IntrospectionException
import jsonrpc.JsonRpcHandler
import jsonrpc.handler.HandlerMacros
import jsonrpc.spi.Codec

trait HandlerBindings[Node, CodecType <: Codec[Node], Effect[_], Context]
  extends Handler[Node, CodecType, Effect, Context]:
  this: JsonRpcHandler[Node, CodecType, Effect, Context] =>

  override inline def bind[T <: AnyRef](api: T): JsonRpcHandler[Node, CodecType, Effect, Context] = bind(api, name => Seq(name))

  override inline def bind[T <: AnyRef](
    api: T,
    exposedNames: String => Seq[String]
  ): JsonRpcHandler[Node, CodecType, Effect, Context] =
    bind(api, Function.unlift(name => Some(exposedNames(name))))

  override inline def bind[T <: AnyRef](
    api: T,
    exposedNames: PartialFunction[String, Seq[String]]
  ): JsonRpcHandler[Node, CodecType, Effect, Context] =
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
