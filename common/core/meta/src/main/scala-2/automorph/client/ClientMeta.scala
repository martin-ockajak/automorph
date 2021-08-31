package automorph.client

import automorph.Client
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Client method bindings code generation.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait ClientMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context] {
  this: Client[Node, Codec, Effect, Context] =>

  /**
   * Creates a remote API proxy instance with RPC bindings for all valid public methods of the specified API type.
   *
   * An API method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not EmptyContext) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting o
ne
   * the caller-supplied ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * Invoked method arguments are supplied ''by name'' as an object.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return remote API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bind[Api <: AnyRef]: Api = macro ClientMeta.bindNamedMacro[Node, Codec, Effect, Context, Api]

  /**
   * Creates a remote API proxy instance with RPC bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not EmptyContext) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting one
   * the caller-supplied ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * Invoked method arguments are supplied ''by position'' as an array.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return remote API proxy instance
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  def bindPositional[Api <: AnyRef]: Api = macro ClientMeta.bindPositionalMacro[Node, Codec, Effect, Context, Api]
}

object ClientMeta {
  /** Client with arbitrary codec. */
  type AnyCodec[Effect[_], Context] = Client[Node, _ <: MessageCodec[Node], Effect, Context] forSome { type Node }

  def bindNamedMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[Api](q"""
      automorph.client.ClientBind.generalBind[
        ${weakTypeOf[Node]},
        ${weakTypeOf[Codec]},
        ${weakTypeOf[Effect[_]]},
        ${weakTypeOf[Context]},
        ${weakTypeOf[Api]}
      ](${c.prefix}, ${c.prefix}.codec, true)
    """)
  }

  def bindPositionalMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[Api](q"""
      automorph.client.ClientBind.generalBind[
        ${weakTypeOf[Node]},
        ${weakTypeOf[Codec]},
        ${weakTypeOf[Effect[_]]},
        ${weakTypeOf[Context]},
        ${weakTypeOf[Api]}
      ](${c.prefix}, ${c.prefix}.codec, false)
    """)
  }

  def generalBind[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    core: ClientCore[Node, Codec, Effect, Context],
    codec: Codec,
    namedArguments: Boolean
  ): Api = macro generalBindMacro[Node, Codec, Effect, Context, Api]

  def generalBindMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    core: c.Expr[ClientCore[Node, Codec, Effect, Context]],
    codec: c.Expr[Codec],
    namedArguments: c.Expr[Boolean]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[Codec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Api](q"""
      // Generate API method bindings
      val methodBindings =
        automorph.client.ClientBindings.generate[$nodeType, $codecType, $effectType, $contextType, $apiType]($codec)

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
            val parameterNames = clientBinding.method.parameters.map(_.name)
            val argumentNames = Option.when($namedArguments)(parameterNames)

            // Perform the API call
            $core.call(method.getName, argumentNames, encodedArguments, resultNode =>
              clientBinding.decodeResult(resultNode), context)
          }.getOrElse(throw new UnsupportedOperationException("Invalid method: " + method.getName))
      ).asInstanceOf[$apiType]
    """)
  }
}
