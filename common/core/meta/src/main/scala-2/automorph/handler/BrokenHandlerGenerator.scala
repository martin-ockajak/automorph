package automorph.handler

import automorph.log.MacroLogger
import automorph.spi.{EffectSystem, MessageCodec}
import automorph.util.MethodReflection.methodLiftable
import automorph.util.{Method, MethodReflection, Reflection}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC handler layer bindings code generation. */
case object BrokenHandlerGenerator {

  def bindings[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    codec: Codec,
    system: EffectSystem[Effect],
    api: Api
  ): Map[String, HandlerBinding[Node, Effect, Context]] = macro bindingsMacro[Node, Codec, Effect, Context, Api]

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
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Map[String, HandlerBinding[Node, Effect, Context]]] = {
    import c.universe.Quasiquote
    val ref = Reflection[c.type](c)

    // Detect and validate public methods in the API type
    val validMethods = MethodReflection.apiMethods[c.type, Api, Effect[_]](ref).map(_.getOrElse(???))

    // Generate bound API method bindings
    val handlerBindings = validMethods.map { method =>
      q"${method.name} -> ${generateBinding[c.type, Node, Codec, Effect, Context, Api](ref)(method, codec, system, api)}"
    }
    c.Expr[Map[String, HandlerBinding[Node, Effect, Context]]](q"""
      Seq(..$handlerBindings).toMap
    """)
  }

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
    implicit val methodLift: Liftable[Method] = methodLiftable(ref)
    Seq(methodLift)
    ref.c.Expr[HandlerBinding[Node, Effect, Context]](q"""
      automorph.handler.HandlerBinding(
        ${method.lift},
        $invoke,
        ${MethodReflection.usesContext[C, Context](ref)(method)}
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
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[(Seq[Node], Context) => Effect[Node]] = {
    import ref.c.universe.{Quasiquote, weakTypeOf}
    (weakTypeOf[Node], weakTypeOf[Codec])

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }

    // Create invoke function
    //   (argumentNodes: Seq[Node], context: Context) => Effect[Node]
    val nodeType = weakTypeOf[Node].dealias
    val contextType = weakTypeOf[Context].dealias
    ref.c.Expr[(Seq[Node], Context) => Effect[Node]](q"""
      (argumentNodes: Seq[$nodeType], context: $contextType) => ${
      // Create the method argument lists by decoding corresponding argument nodes into values
      //   List(List(
      //     (Try(codec.decode[Parameter0Type](argumentNodes(0))) match {
      //       case Failure(error) => Failure(InvalidRequestException("Invalid argument: " + ${ Expr(argumentIndex) }, error))
      //       case result => result
      //     }).get
      //     ...
      //     (Try(codec.decode[ParameterNType](argumentNodes(N))) match {
      //       case Failure(error) => Failure(InvalidRequestException("Invalid argument: " + ${ Expr(argumentIndex) }, error))
      //       case result => result
      //     }).get
      //   )): List[List[ParameterXType]]
      val arguments = method.parameters.toList.zip(parameterListOffsets).map { case (parameters, offset) =>
        parameters.toList.zipWithIndex.map { case (parameter, index) =>
          val argumentIndex = offset + index
          val decode = q"""
            $codec.decode[${parameter.dataType}](argumentNodes($argumentIndex))
           """
          println(ref.c.universe.showCode(decode))
          decode
        }
      }

      // Create the API method call using the decoded arguments
      //   api.method(arguments ...): Effect[ResultValueType]
      val apiMethodCall = q"$api.${method.symbol}(...$arguments)"
      println(ref.c.universe.showCode(apiMethodCall))

      // Create encode result function
      //   (result: ResultValueType) => Node = codec.encode[ResultValueType](result)
      val resultValueType = MethodReflection.unwrapType[C, Effect[_]](ref.c)(method.resultType).dealias
      val encodeResult = q"(result: $resultValueType) => $codec.encode[$resultValueType](result)"

      // Create the effect mapping call using the method call and the encode result function
      //   system.map(methodCall, encodeResult): Effect[Node]
      q"$system.map($apiMethodCall, $encodeResult)"
      //      arguments.flatten.foreach(println(ref.c.universe.showCode(_)))
      //      q"null.asInstanceOf[$effectType[$resultValueType]]"
    }
    """)
  }

  private def logBoundMethod[C <: blackbox.Context, Api: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod,
    invoke: ref.c.Expr[Any]
  ): Unit = MacroLogger.debug(
    s"""${methodSignature[C, Api](ref)(method)} =
      |  ${ref.c.universe.showCode(invoke.tree)}
      |""".stripMargin
  )
}
