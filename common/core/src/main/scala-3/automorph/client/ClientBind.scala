package automorph.client

import automorph.Client
import automorph.client.{ClientBindings, ClientCore}
import automorph.spi.MessageFormat
import java.lang.reflect.Proxy
import scala.compiletime.summonInline
import scala.reflect.ClassTag

/**
 * Client method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait ClientBind[Node, Format <: MessageFormat[Node], Effect[_], Context]:

  def core: ClientCore[Node, Format, Effect, Context]

  /**
   * Creates a JSON-RPC API proxy instance with bindings for all valid public methods of the specified API.
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
   * Invoked method arguments are supplied ''by name'' as an object.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return JSON-RPC API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef]: Api =
    ClientBind.generalBind[Node, Format, Effect, Context, Api](core, namedArguments = true)

  /**
   * Creates a JSON-RPC API proxy instance with bindings for all valid public methods of the specified API.
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
  inline def bindPositional[Api <: AnyRef]: Api =
    ClientBind.generalBind[Node, Format, Effect, Context, Api](core, namedArguments = false)

object ClientBind:

  /** Client with arbitrary format. */
  type AnyFormat[Effect[_], Context] = Client[_, _, Effect, Context]

  inline def generalBind[Node, Format <: MessageFormat[Node], Effect[_], Context, Api <: AnyRef](
    clientCore: ClientCore[Node, Format, Effect, Context],
    namedArguments: Boolean
  ): Api =
    // Generate API method bindings
    val methodBindings = ClientBindings.generate[Node, Format, Effect, Context, Api](clientCore.format)

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
          val parameterNames = clientBinding.method.parameters.flatten.map(_.name)
          val argumentNames = Option.when(namedArguments)(parameterNames)

          // Perform the API call
          clientCore.call(
            method.getName,
            argumentNames,
            encodedArguments,
            resultNode => clientBinding.decodeResult(resultNode),
            context
          )
        }.getOrElse(throw UnsupportedOperationException(s"Invalid method: ${method.getName}"))
    ).asInstanceOf[Api]
