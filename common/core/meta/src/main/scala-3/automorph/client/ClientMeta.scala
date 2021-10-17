package automorph.client

import automorph.Client
import automorph.client.ClientGenerator
import automorph.spi.MessageCodec
import java.lang.reflect.Proxy
import scala.compiletime.summonInline
import scala.reflect.ClassTag

/**
 * Client method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait ClientMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context]:
  this: Client[Node, Codec, Effect, Context] =>

  /**
   * Creates a remote API proxy instance with RPC bindings for all valid public methods of the specified API type.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Context.Empty) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the caller-supplied request context is passed to the underlying message transport plugin.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return remote API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef]: Api = ClientMeta.generalBind[Node, Codec, Effect, Context, Api](this, protocol.codec)

object ClientMeta:

  /** Client with arbitrary codec. */
  type AnyCodec[Effect[_], Context] = Client[_, _, Effect, Context]

  inline def generalBind[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    core: Client[Node, Codec, Effect, Context],
    codec: Codec
  ): Api =
    // Generate API method bindings
    val methodBindings = ClientGenerator.bindings[Node, Codec, Effect, Context, Api](codec).map { binding =>
      binding.function.name -> binding
    }.toMap

    // Create API proxy instance
    val classTag = summonInline[ClassTag[Api]]
    Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array(classTag.runtimeClass),
      (_, method, arguments) =>
        // Lookup bindings for the specified method
        methodBindings.get(method.getName).map { clientBinding =>
          // Adjust expected method parameters if it uses context as its last parameter
          val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
          val (argumentValues, context) =
            if clientBinding.usesContext && callArguments.nonEmpty then
              callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[Context])
            else
              callArguments.toSeq -> None

          // Encode method arguments
          val encodedArguments = clientBinding.encodeArguments(argumentValues)
          val parameterNames = clientBinding.function.parameters.map(_.name)

          // Perform the API call
          core.call(
            method.getName,
            parameterNames,
            encodedArguments,
            resultNode => clientBinding.decodeResult(resultNode),
            context
          )
        }.getOrElse(throw UnsupportedOperationException(s"Invalid method: ${method.getName}"))
    ).asInstanceOf[Api]
