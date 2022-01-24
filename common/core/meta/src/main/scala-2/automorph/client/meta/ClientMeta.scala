package automorph.client.meta

import automorph.client.RemoteCall
import automorph.spi.{MessageCodec, RpcProtocol}
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

  def protocol: RpcProtocol[Node, Codec, Context]

  /**
   * Creates a RPC API proxy instance with RPC bindings for all valid public functions of the specified API type.
   *
   * An API function is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if message context type is not Context.Empty) accepts the specified message context type as its last parameter
   *
   * If a bound function definition contains a last parameter of `Context` type or returns a context function accepting one
   * the caller-supplied request context is passed to the underlying message transport plugin.
   *
   * RPC functions represented by bound API methods are invoked using their actual names.
   *
   * @tparam Api API trait type (classes are not supported)
   * @return RPC API proxy instance
   * @throws java.lang.IllegalArgumentException if invalid public functions are found in the API type
   */
  def bind[Api <: AnyRef]: Api =
    macro ClientMeta.bindMacro[Node, Codec, Effect, Context, Api]

  /**
   * Creates a remote API proxy instance with RPC bindings for all valid public functions of the specified API type.
   *
   * An API function is considered valid if it satisfies all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if message context type is not Context.Empty) accepts the specified message context type as its last parameter
   *
   * If a bound function definition contains a last parameter of `Context` type or returns a context function accepting one
   * the caller-supplied request context is passed to the underlying message transport plugin.
   *
   * RPC functions represented by bound API methods are invoked using their names transformed via the `mapName` function.
   *
   * @param mapName maps API method name to the invoked RPC function name
   * @tparam Api remote API trait type (classes are not supported)
   * @return remote API proxy instance
   * @throws java.lang.IllegalArgumentException if invalid public functions are found in the API type
   */
  def bind[Api <: AnyRef](mapName: String => String): Api =
    macro ClientMeta.bindMapNamesMacro[Node, Codec, Effect, Context, Api]

  /**
   * Creates a remote API function call proxy.
   *
   * Uses the remote function name and arguments to send an RPC request and
   * extracts a result value or an error from the received RPC response.
   *
   * @param function remote function name
   * @tparam Result result type
   * @return specified remote function call proxy
   */
  def call[Result](function: String): RemoteCall[Node, Codec, Effect, Context, Result] =
    macro ClientMeta.callMacro[Node, Codec, Effect, Context, Result]

  def performCall[Result](
    function: String,
    arguments: Seq[(String, Node)],
    decodeResult: (Node, Context) => Result,
    requestContext: Option[Context]
  ): Effect[Result]
}

object ClientMeta {

  def bindMacro[
    Node,
    Codec <: MessageCodec[Node],
    Effect[_],
    Context,
    Api <: AnyRef
  ](c: blackbox.Context): c.Expr[Api] = {
    import c.universe.Quasiquote

    c.Expr[Api](q"""
      ${c.prefix}.bind(identity)
    """)
  }

  def bindMapNamesMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    mapName: c.Expr[String => String]
  )(implicit effectType: c.WeakTypeTag[Effect[?]]): c.Expr[Api] = {
    import c.universe.{Quasiquote, weakTypeOf}

    val nodeType = weakTypeOf[Node]
    val codecType = weakTypeOf[Codec]
    val contextType = weakTypeOf[Context]
    val apiType = weakTypeOf[Api]
    c.Expr[Api](q"""
      // Generate API function bindings
      val bindings = automorph.client.meta.ClientGenerator
        .bindings[$nodeType, $codecType, $effectType, $contextType, $apiType](${c.prefix}.protocol.codec).map { binding =>
          binding.function.name -> binding
        }.toMap

      // Create API proxy instance
      java.lang.reflect.Proxy.newProxyInstance(
        getClass.getClassLoader,
        Array(classOf[$apiType]),
        (_, method, arguments) =>
          // Lookup bindings for the specified method
          bindings.get(method.getName).map { binding =>
            // Adjust RPC function arguments if it accepts request context as its last parameter
            val callArguments = Option(arguments).getOrElse(Array.empty[AnyRef])
            val (argumentValues, requestContext) =
              if (binding.acceptsContext && callArguments.nonEmpty) {
                callArguments.dropRight(1).toSeq -> Some(callArguments.last.asInstanceOf[$contextType])
              } else {
                callArguments.toSeq -> None
              }

            // Encode RPC function arguments
            val argumentNodes = binding.function.parameters.zip(argumentValues).map { case (parameter, argument) =>
              val encodeArgument = binding.argumentEncoders.getOrElse(
                parameter.name,
                throw new IllegalStateException("Missing method parameter encoder: " + parameter.name)
              )
              parameter.name -> scala.util.Try(encodeArgument(argument)).recoverWith { case error =>
                scala.util.Failure(automorph.spi.RpcProtocol.InvalidRequestException(
                  "Malformed argument: " + parameter.name,
                  error
                ))
              }.get
            }

            // Perform the RPC call
            ${c.prefix}.performCall(
              $mapName(method.getName),
              argumentNodes,
              (resultNode, responseContext) => binding.decodeResult(resultNode, responseContext),
              requestContext)
          }.getOrElse(throw new UnsupportedOperationException("Invalid method: " + method.getName))
      ).asInstanceOf[$apiType]
    """)
  }

  def callMacro[
    Node,
    Codec <: MessageCodec[Node],
    Effect[_],
    Context,
    Result
  ](c: blackbox.Context)(function: c.Expr[String]): c.Expr[RemoteCall[Node, Codec, Effect, Context, Result]] = {
    import c.universe.Quasiquote

    c.Expr[RemoteCall[Node, Codec, Effect, Context, Result]](q"""
      automorph.client.RemoteCall($function, ${c.prefix}.protocol.codec, ${c.prefix}.performCall)
    """)
  }
}
