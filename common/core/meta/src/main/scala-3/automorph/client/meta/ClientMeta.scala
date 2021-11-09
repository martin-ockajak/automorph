package automorph.client.meta

import automorph.client.{RemoteCall, RemoteMessage}
import automorph.spi.{MessageCodec, RpcProtocol}
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

  def protocol: RpcProtocol[Node, Codec, Context]

  /**
   * Creates a RPC API proxy instance with RPC bindings for all valid public methods of the specified API type.
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
   * RPC functions represented by bound API methods are invoked using their name transformed via the `mapName` function.
   *
   * @param mapName maps API method name to the invoked RPC function name
   * @tparam Api API trait type (classes are not supported)
   * @return RPC API proxy instance
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
          val argumentNodes = clientBinding.encodeArguments(argumentValues)
          val parameterNames = clientBinding.function.parameters.map(_.name)

          // Perform the API call
          performCall(
            method.getName,
            parameterNames,
            argumentNodes,
            (resultNode, responseContext) => clientBinding.decodeResult(resultNode, responseContext),
            requestContext
          )
        }.getOrElse(throw UnsupportedOperationException(s"Invalid method: ${method.getName}"))
    ).asInstanceOf[Api]

  /**
   * Prepares an remote API function call.
   *
   * RPC function name and call arguments are used to send an RPC request and
   * a result value or an error is extracted from the received RPC response.
   *
   * @param function RPC function name
   * @tparam Result result type
   * @return RPC function call proxy with specified function name
   */
  inline def call[Result](function: String): RemoteCall[Node, Codec, Effect, Context, Result] =
    RemoteCall(function, protocol.codec, performCall)

  def performCall[Result](
    function: String,
    argumentNames: Seq[String],
    argumentNodes: Seq[Node],
    decodeResult: (Node, Context) => Result,
    requestContext: Option[Context]
  ): Effect[Result]
