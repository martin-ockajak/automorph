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
 * @tparam Context message context type
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
   * - (if message context type is not Context.Empty) accepts the specified message context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the caller-supplied request context is passed to the underlying message transport plugin.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return remote API proxy instance
   * @throws java.lang.IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef]: Api =
    // Generate API method bindings
    val methodBindings = ClientGenerator
      .bindings[Node, Codec, Effect, Context, Api](protocol.codec).map { binding =>
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
          // Adjust expected method parameters if it accepts request context as its last parameter
          val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
          val (argumentValues, requestContext) =
            if clientBinding.acceptsContext && callArguments.nonEmpty then
              callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[Context])
            else
              callArguments.toSeq -> None

          // Encode method arguments
          val encodedArguments = clientBinding.encodeArguments(argumentValues)
          val parameterNames = clientBinding.function.parameters.map(_.name)

          // Perform the API call
          call(
            method.getName,
            parameterNames,
            encodedArguments,
            (resultNode, responseContext) => clientBinding.decodeResult(resultNode, responseContext),
            requestContext
          )
        }.getOrElse(throw UnsupportedOperationException(s"Invalid method: ${method.getName}"))
    ).asInstanceOf[Api]
