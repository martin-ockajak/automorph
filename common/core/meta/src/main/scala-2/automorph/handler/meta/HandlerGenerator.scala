package automorph.handler.meta

import automorph.Contextual
import automorph.handler.HandlerBinding
import automorph.log.MacroLogger
import automorph.spi.protocol.RpcFunction
import automorph.spi.{EffectSystem, MessageCodec}
import automorph.util.{MethodReflection, Reflection}
import scala.annotation.nowarn
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** RPC handler layer bindings code generation. */
object HandlerGenerator {

  /**
   * Generates handler bindings for all valid public methods of an API type.
   *
   * @param codec message codec plugin
   * @param system effect system plugin
   * @param api API instance
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context message context type
   * @tparam Api API type
   * @return mapping of API method names to handler function bindings
   */
  def bindings[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    codec: Codec,
    system: EffectSystem[Effect],
    api: Api
  ): Seq[HandlerBinding[Node, Effect, Context]] = macro bindingsMacro[Node, Codec, Effect, Context, Api]

  def bindingsMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    codec: c.Expr[Codec],
    system: c.Expr[EffectSystem[Effect]],
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Seq[HandlerBinding[Node, Effect, Context]]] = {
    import c.universe.Quasiquote
    val ref = Reflection[c.type](c)

    // Detect and validate public methods in the API type
    val apiMethods = MethodReflection.apiMethods[c.type, Api, Effect[_]](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match {
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors =>
        ref.c.abort(
          ref.c.enclosingPosition,
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )
    }

    // Generate bound API method bindings
    val handlerBindings = validMethods.map { method =>
      generateBinding[c.type, Node, Codec, Effect, Context, Api](ref)(method, codec, system, api)
    }
    c.Expr[Seq[HandlerBinding[Node, Effect, Context]]](q"""
      Seq(..$handlerBindings)
    """)
  }

  @nowarn("msg=used")
  private def generateBinding[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api: ref.c.WeakTypeTag
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[Codec],
    system: ref.c.Expr[EffectSystem[Effect]],
    api: ref.c.Expr[Api]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[HandlerBinding[Node, Effect, Context]] = {
    import ref.c.universe.{Liftable, Quasiquote}

    val invoke = generateInvoke[C, Node, Codec, Effect, Context, Api](ref)(method, codec, system, api)
    logBoundMethod[C, Api](ref)(method, invoke)
    implicit val functionLiftable: Liftable[RpcFunction] = MethodReflection.functionLiftable(ref)
    ref.c.Expr[HandlerBinding[Node, Effect, Context]](q"""
      automorph.handler.HandlerBinding(
        ${method.lift.rpcFunction},
        $invoke,
        ${MethodReflection.acceptsContext[C, Context](ref)(method)}
      )
    """)
  }

  private def generateInvoke[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[Codec],
    system: ref.c.Expr[EffectSystem[Effect]],
    api: ref.c.Expr[Api]
  )(implicit
    effectType: ref.c.WeakTypeTag[Effect[_]]
  ): ref.c.Expr[(Seq[Option[Node]], Context) => Effect[(Node, Option[Context])]] = {
    import ref.c.universe.{Quasiquote, weakTypeOf}
    (weakTypeOf[Node], weakTypeOf[Codec])

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create invoke function
    //   (argumentNodes: Seq[Option[Node]], requestContext: Context) => Effect[Node]
    val nodeType = weakTypeOf[Node].dealias
    val contextType = weakTypeOf[Context].dealias
    ref.c.Expr[(Seq[Option[Node]], Context) => Effect[(Node, Option[Context])]](q"""
      (argumentNodes: Seq[Option[$nodeType]], requestContext: $contextType) => ${
      // Create the method argument lists by decoding corresponding argument nodes into values
      //   List(List(
      //     (Try(Option(codec.decode[Parameter0Type](argumentNodes(0).getOrElse(codec.encode(None)))).get) match {
      //       ... error handling ...
      //     }).get
      //     ...
      //     (Try(Option(codec.decode[ParameterNType](argumentNodes(N).getOrElse(codec.encode(None)))).get) match {
      //       ... error handling ...
      //     }).get
      //   )): List[List[ParameterXType]]
      val apiMethodArguments = method.parameters.toList.zip(parameterListOffsets).map { case (parameters, offset) =>
        parameters.toList.zipWithIndex.map { case (parameter, index) =>
          val argumentIndex = offset + index
          if (argumentIndex == lastArgumentIndex && MethodReflection.acceptsContext[C, Context](ref)(method)) {
            // Use supplied request context as a last argument if the method accepts context as its last parameter
            q"requestContext"
          } else {
            // Decode an argument node if present or otherwise an empty node into a value
            q"""
              (scala.util.Try(Option($codec.decode[${parameter.dataType}](
                argumentNodes($argumentIndex).getOrElse($codec.encode(None))
              )).get) match {
                case scala.util.Failure(error) => scala.util.Failure(
                  automorph.spi.RpcProtocol.InvalidRequestException(
                    argumentNodes($argumentIndex).fold("Missing")(_ => "Malformed") + " argument: " + ${parameter.name},
                    error
                  )
                )
                case result => result
              }).get
            """
          }
        }
      }

      // Create the API method call using the decoded arguments
      //   api.method(arguments ...): Effect[ResultType]
      val apiMethodCall = q"$api.${method.symbol}(...$apiMethodArguments)"

      // Create encode result function
      //   (result: ResultType) => Node = codec.encode[ResultType](result) -> Option.empty[Context]
      val resultType = MethodReflection.unwrapType[C, Effect[_]](ref.c)(method.resultType).dealias
      val encodeResult = MethodReflection.contextualResult[C, Context, Contextual[_, _]](ref.c)(resultType)
        .map { contextualResultType =>
          q"""
            (result: Contextual[$contextualResultType, $contextType]) =>
              $codec.encode[$contextualResultType](result.result) -> Some(result.context)
          """
        }.getOrElse {
          q"""
            (result: $resultType) =>
              $codec.encode[$resultType](result) -> Option.empty[$contextType]
          """
        }

      // Create the effect mapping call using the method call and the encode result function
      //   system.map(apiMethodCall, encodeResult): Effect[(Node, Option[Context])]
      q"$system.map($apiMethodCall, $encodeResult)"
    }
    """)
  }

  private def logBoundMethod[C <: blackbox.Context, Api: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod,
    invoke: ref.c.Expr[Any]
  ): Unit = MacroLogger.debug(
    s"""${MethodReflection.signature[C, Api](ref)(method)} =
      |  ${ref.c.universe.showCode(invoke.tree)}
      |""".stripMargin
  )
}
