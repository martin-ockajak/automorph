package automorph.handler

import automorph.log.MacroLogger
import automorph.protocol.MethodBindings.{methodLiftable, methodSignature, methodUsesContext, unwrapType, validApiMethods}
import automorph.spi.{EffectSystem, MessageFormat}
import automorph.util.{Method, Reflection}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC handler layer bindings code generation. */
case object BrokenHandlerBindings {

  def generate[Node, Format <: MessageFormat[Node], Effect[_], Context, Api <: AnyRef](
    format: Format,
    system: EffectSystem[Effect],
    api: Api
  ): Map[String, HandlerBinding[Node, Effect, Context]] = macro generateMacro[Node, Format, Effect, Context, Api]

  def generateMacro[
    Node: c.WeakTypeTag,
    Format <: MessageFormat[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    format: c.Expr[Format],
    system: c.Expr[EffectSystem[Effect]],
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Map[String, HandlerBinding[Node, Effect, Context]]] = {
    import c.universe.Quasiquote
    val ref = Reflection[c.type](c)

    // Detect and validate public methods in the API type
    val validMethods = validApiMethods[c.type, Api, Effect[_]](ref).map(_.getOrElse(???))

    // Generate bound API method bindings
    val handlerBindings = validMethods.map { method =>
      q"${method.name} -> ${generateBinding[c.type, Node, Format, Effect, Context, Api](ref)(method, format, system, api)}"
    }
    c.Expr[Map[String, HandlerBinding[Node, Effect, Context]]](q"""
      Seq(..$handlerBindings).toMap
    """)
  }

  private def generateBinding[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Format <: MessageFormat[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api: ref.c.WeakTypeTag
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    format: ref.c.Expr[Format],
    system: ref.c.Expr[EffectSystem[Effect]],
    api: ref.c.Expr[Api]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[HandlerBinding[Node, Effect, Context]] = {
    import ref.c.universe.{Liftable, Quasiquote}

    val invoke = generateInvoke[C, Node, Format, Effect, Context, Api](ref)(method, format, system, api)
    logBoundMethod[C, Api](ref)(method, invoke)
    implicit val methodLift: Liftable[Method] = methodLiftable(ref)
    Seq(methodLift)
    ref.c.Expr[HandlerBinding[Node, Effect, Context]](q"""
      automorph.handler.HandlerBinding(
        ${method.lift},
        $invoke,
        ${methodUsesContext[C, Context](ref)(method)}
      )
    """)
  }

  private def generateInvoke[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Format <: MessageFormat[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    format: ref.c.Expr[Format],
    system: ref.c.Expr[EffectSystem[Effect]],
    api: ref.c.Expr[Api]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[(Seq[Node], Context) => Effect[Node]] = {
    import ref.c.universe.{weakTypeOf, Quasiquote}
    (weakTypeOf[Node], weakTypeOf[Format])

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
      //     (Try(format.decode[Parameter0Type](argumentNodes(0))) match {
      //       case Failure(error) => Failure(InvalidRequestException("Invalid argument: " + ${ Expr(argumentIndex) }, error))
      //       case result => result
      //     }).get
      //     ...
      //     (Try(format.decode[ParameterNType](argumentNodes(N))) match {
      //       case Failure(error) => Failure(InvalidRequestException("Invalid argument: " + ${ Expr(argumentIndex) }, error))
      //       case result => result
      //     }).get
      //   )): List[List[ParameterXType]]
      val arguments = method.parameters.toList.zip(parameterListOffsets).map { case (parameters, offset) =>
        parameters.toList.zipWithIndex.map { case (parameter, index) =>
          val argumentIndex = offset + index
          val decode = q"""
            $format.decode[${parameter.dataType}](argumentNodes($argumentIndex))
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
      //   (result: ResultValueType) => Node = format.encode[ResultValueType](result)
      val resultValueType = unwrapType[C, Effect[_]](ref.c)(method.resultType).dealias
      val encodeResult = q"(result: $resultValueType) => $format.encode[$resultValueType](result)"

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
