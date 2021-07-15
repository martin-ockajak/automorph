package automorph.client

import automorph.Client
import automorph.spi.MessageFormat
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Client method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam ActualFormat message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait ClientBind[Node, ActualFormat <: MessageFormat[Node], Effect[_], Context] {

  def core: ClientCore[Node, ActualFormat, Effect, Context]

  /**
   * Creates a JSON-RPC API proxy instance with bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting o
ne
   * the caller-supplied ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * Invoked method arguments are supplied ''by name'' as an object.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return JSON-RPC API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef]: Api = macro ClientBind.bindNamedMacro[Node, ActualFormat, Effect, Context, Api]

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
  def bindPositional[Api <: AnyRef]: Api = macro ClientBind.bindPositionalMacro[Node, ActualFormat, Effect, Context, Api]
}

object ClientBind {
  /** Client with arbitrary format. */
  type AnyFormat[Effect[_], Context] = Client[Node, _ <: MessageFormat[Node], Effect, Context] forSome { type Node }

  def bindNamedMacro[
    Node: c.WeakTypeTag,
    ActualFormat <: MessageFormat[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[Api](q"""
      automorph.client.ClientBind.generalBind[
        ${weakTypeOf[Node]},
        ${weakTypeOf[ActualFormat]},
        ${weakTypeOf[Effect[_]]},
        ${weakTypeOf[Context]},
        ${weakTypeOf[Api]}
      ](${c.prefix}.core, true)
    """)
  }

  def bindPositionalMacro[
    Node: c.WeakTypeTag,
    ActualFormat <: MessageFormat[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[Api](q"""
      automorph.client.ClientBind.generalBind[
        ${weakTypeOf[Node]},
        ${weakTypeOf[ActualFormat]},
        ${weakTypeOf[Effect[_]]},
        ${weakTypeOf[Context]},
        ${weakTypeOf[Api]}
      ](${c.prefix}.core, false)
    """)
  }

  def generalBind[Node, ActualFormat <: MessageFormat[Node], Effect[_], Context, Api <: AnyRef](
    clientCore: ClientCore[Node, ActualFormat, Effect, Context],
    namedArguments: Boolean
  ): Api = macro generalBindMacro[Node, ActualFormat, Effect, Context, Api]

  def generalBindMacro[
    Node: c.WeakTypeTag,
    ActualFormat <: MessageFormat[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    clientCore: c.Expr[ClientCore[Node, ActualFormat, Effect, Context]],
    namedArguments: c.Expr[Boolean]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    val nodeType = weakTypeOf[Node]
    val formatType = weakTypeOf[ActualFormat]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Api](q"""
      // Generate API method bindings
      val format = $clientCore.format
      val methodBindings =
        automorph.client.ClientBindings.generate[$nodeType, $formatType, $effectType, $contextType, $apiType](format)

      // Create API proxy instance
      java.lang.reflect.Proxy.newProxyInstance(
        getClass.getClassLoader,
        Array(classOf[$apiType]),
        (_, method, arguments) =>
          // Lookup bindings for the specified method
          methodBindings.get(method.getName).map { clientBinding =>
            // Adjust expected method parameters if it uses context as its last parameter
            val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
            val (argumentValues, context) =
              if (clientBinding.usesContext && callArguments.nonEmpty) {
                callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[$contextType])
              } else {
                callArguments.toSeq -> None
              }

            // Encode method arguments
            val encodedArguments = clientBinding.encodeArguments(argumentValues)
            val parameterNames = clientBinding.method.parameters.flatten.map(_.name)
            val argumentNames = Option.when($namedArguments)(parameterNames)

            // Perform the API call
            $clientCore.call(method.getName, argumentNames, encodedArguments, resultNode =>
              clientBinding.decodeResult(resultNode), context)
          }.getOrElse(throw new UnsupportedOperationException("Invalid method: " + method.getName))
      ).asInstanceOf[$apiType]
    """)
  }
}
