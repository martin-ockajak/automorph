package automorph.client

import java.lang.reflect.Proxy
import automorph.client.ClientBindings
import automorph.spi.Codec
import scala.compiletime.summonInline
import scala.reflect.ClassTag

/**
 * JSON-RPC client layer code generation.
 *
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait ClientMeta[Node, ExactCodec <: Codec[Node], Effect[_], Context]:
  this: ClientCore[Node, ExactCodec, Effect, Context] =>

  /**
   * Create a JSON-RPC API proxy instance with bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the caller-supplied ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * Invoked method arguments are supplied ''by position'' as an array.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return JSON-RPC API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef]: Api =
    // Generate API method bindings
    val methodBindings = ClientBindings.generate[Node, ExactCodec, Effect, Context, Api](codec)

    // Create API proxy instance
    val classTag = summonInline[ClassTag[Api]]
    Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array(classTag.runtimeClass),
      (_, method, arguments) =>
        // Lookup bindings for the specified method
        methodBindings.get(method.getName).map { clientMethod =>
          // Adjust expected method parameters if it uses context as its last parameter
          val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
          val (argumentValues, context) =
            if clientMethod.usesContext && callArguments.nonEmpty then
              callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[Context])
            else
              callArguments.toSeq -> None

          // Encode method arguments
          val encodedArguments = clientMethod.encodeArguments(argumentValues)
          val argumentNames = Option.when(namedArguments)(clientMethod.paramNames)

          // Perform the API call
          performCall(method.getName, argumentNames, encodedArguments, resultNode => clientMethod.decodeResult(resultNode), context)
        }.getOrElse(throw UnsupportedOperationException(s"Invalid method: ${method.getName}"))
    ).asInstanceOf[Api]
