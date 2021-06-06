package jsonrpc.handler

import java.beans.IntrospectionException
import jsonrpc.handler.HandlerMacros
import jsonrpc.spi.{Backend, Codec}

trait HandlerBindings[Node, CodecType <: Codec[Node], Effect[_], Context]
  extends Handler[Node, CodecType, Effect, Context]:

  override inline def bind[T <: AnyRef](api: T): Handler[Node, CodecType, Effect, Context] =
    bind(api, name => Seq(name))

  override inline def bind[T <: AnyRef](
    api: T,
    exposedNames: String => Seq[String]
  ): Handler[Node, CodecType, Effect, Context] =
    bind(api, Function.unlift(name => Some(exposedNames(name))))

  override inline def bind[T <: AnyRef](
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
    clone(bindings)

  protected def clone(
    extraMethodBindings: Map[String, MethodHandle[Node, Effect, Context]]
  ): Handler[Node, CodecType, Effect, Context]
