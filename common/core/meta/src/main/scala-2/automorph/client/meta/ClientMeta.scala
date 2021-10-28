package automorph.client.meta

import automorph.Client
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Client function bindings code generation.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
private[automorph] trait ClientMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context] {
  this: Client[Node, Codec, Effect, Context] =>

  /**
   * Creates a remote API proxy instance with RPC bindings for all valid public functions of the specified API type.
   *
   * An API function is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if message context type is not Context.Empty) accepts the specified message context type as its last parameter
   *
   * If a bound function definition contains a last parameter of `Context` type or returns a context function accepting o
   * ne
   * the caller-supplied request context is passed to the underlying message transport plugin.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return remote API proxy instance
   * @throws java.lang.IllegalArgumentException if invalid public functions are found in the API type
   */
  def bind[Api <: AnyRef]: Api = macro ClientMeta.bindMacro[Node, Codec, Effect, Context, Api]
}

object ClientMeta {

  def bindMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[Codec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Api](q"""
      // Generate API function bindings
      val functionBindings = automorph.client.ClientGenerator
        .bindings[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.protocol.codec).map { binding =>
          binding.function.name -> binding
        }.toMap

      // Create API proxy instance
      java.lang.reflect.Proxy.newProxyInstance(
        getClass.getClassLoader,
        Array(classOf[$apiType]),
        (_, function, arguments) =>
          // Lookup bindings for the specified function
          functionBindings.get(function.getName).map { clientBinding =>
            // Adjust expected function parameters if it accepts request context as its last parameter
            val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
            val (argumentValues, requestContext) =
              if (clientBinding.acceptsContext && callArguments.nonEmpty) {
                callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[$contextType])
              } else {
                callArguments.toSeq -> None
              }

            // Encode function arguments
            val encodedArguments = clientBinding.encodeArguments(argumentValues)
            val parameterNames = clientBinding.function.parameters.map(_.name)

            // Perform the API call
            ${c.prefix}.call(function.getName, parameterNames, encodedArguments,
              (resultNode, responseContext) => clientBinding.decodeResult(resultNode, responseContext),
              requestContext)
          }.getOrElse(throw new UnsupportedOperationException("Invalid function: " + function.getName))
      ).asInstanceOf[$apiType]
    """)
  }
}
