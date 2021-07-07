package automorph.client

import automorph.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Client method bindings code generation.
 *
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait ClientBind[Node, ExactCodec <: Codec[Node], Effect[_], Context] {
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
  def bind[Api <: AnyRef]: Api = macro ClientBind.bindNamedMacro[Node, ExactCodec, Effect, Context, Api]

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
   def bindPositional[Api <: AnyRef]: Api = macro ClientBind.bindPositionalMacro[Node, ExactCodec, Effect, Context, Api]

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
   * @param namedArguments if true, invoked method orguments are supplied ''by name'' as an object, otherwise ''by position'' as an array
   * @tparam Api API trait type (classes are not supported)
   * @return JSON-RPC API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef](namedArguments: Boolean): Api = macro ClientBind.bindMacro[Node, ExactCodec, Effect, Context, Api]
}

object ClientBind {
  def bindNamedMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Api](q"""
      ${c.prefix}.bind[
        ${weakTypeOf[Node]},
        ${weakTypeOf[ExactCodec]},
        ${weakTypeOf[Effect[_]]},
        ${weakTypeOf[Context]},
        ${weakTypeOf[Api]}
      ](namedArguments = true)
    """)
  }

  def bindPositionalMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[Api](q"""
      ${c.prefix}.bind[
        ${weakTypeOf[Node]},
        ${weakTypeOf[ExactCodec]},
        ${weakTypeOf[Effect[_]]},
        ${weakTypeOf[Context]},
        ${weakTypeOf[Api]}
      ](namedArguments = false)
    """)
  }

  def bindMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    namedArguments: c.Expr[Boolean]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[ExactCodec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Api](q"""
      // Generate API method bindings
      val codec = ${c.prefix}.codec
      val methodBindings =
        automorph.client.ClientBindings.generate[$nodeType, $codecType, $effectType, $contextType, $apiType](codec)

      // Create API proxy instance
      java.lang.reflect.Proxy.newProxyInstance(
        getClass.getClassLoader,
        Array(classOf[$apiType]),
        (_, method, arguments) =>
          // Lookup bindings for the specified method
          methodBindings.get(method.getName).map { clientMethod =>
            // Adjust expected method parameters if it uses context as its last parameter
            val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
            val (argumentValues, context) =
              if (clientMethod.usesContext && callArguments.nonEmpty) {
                callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[$contextType])
              } else {
                callArguments.toSeq -> None
              }

            // Encode method arguments
            val encodedArguments = clientMethod.encodeArguments(argumentValues)
            val parameterNames = clientMethod.method.parameters.flatten.map(_.name)
            val argumentNames = Option.when($namedArguments)(parameterNames)

            // Perform the API call
            ${c.prefix}.performCall(method.getName, argumentNames, encodedArguments, resultNode => clientMethod.decodeResult(resultNode), context)
          }.getOrElse(throw new UnsupportedOperationException("Invalid method: " + method.getName))
      ).asInstanceOf[$apiType]
    """)
  }
}
