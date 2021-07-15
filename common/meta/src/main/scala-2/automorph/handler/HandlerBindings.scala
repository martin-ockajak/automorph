package automorph.handler

import automorph.protocol.MethodBindings.{methodLiftable, methodSignature, methodUsesContext, unwrapType, validApiMethods}
import automorph.spi.{EffectSystem, MessageFormat}
import automorph.util.{Method, Reflection}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC handler layer bindings code generation. */
case object HandlerBindings {

  private val debugProperty = "automorph.macro.debug"

  /**
   * Generates handler bindings for all valid public methods of an API type.
   *
   * @param format message format plugin
   * @param system effect system plugin
   * @param api API instance
   * @tparam Node message node type
   * @tparam ActualFormat message format plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam Api API type
   * @return mapping of method names to handler method bindings
   */
  def generate[Node, ActualFormat <: MessageFormat[Node], Effect[_], Context, Api <: AnyRef](
    format: ActualFormat,
    system: EffectSystem[Effect],
    api: Api
  ): Map[String, HandlerBinding[Node, Effect, Context]] = macro generateMacro[Node, ActualFormat, Effect, Context, Api]

  def generateMacro[
    Node: c.WeakTypeTag,
    ActualFormat <: MessageFormat[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    format: c.Expr[ActualFormat],
    system: c.Expr[EffectSystem[Effect]],
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Map[String, HandlerBinding[Node, Effect, Context]]] = {
    import c.universe.Quasiquote
    val ref = Reflection[c.type](c)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[c.type, Api, Effect[_]](ref)
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
      q"${method.name} -> ${generateBinding[c.type, Node, ActualFormat, Effect, Context, Api](ref)(method, format, system, api)}"
    }
    c.Expr[Map[String, HandlerBinding[Node, Effect, Context]]](q"""
      Seq(..$handlerBindings).toMap
    """)
  }

  private def generateBinding[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    ActualFormat <: MessageFormat[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api: ref.c.WeakTypeTag
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    format: ref.c.Expr[ActualFormat],
    system: ref.c.Expr[EffectSystem[Effect]],
    api: ref.c.Expr[Api]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[HandlerBinding[Node, Effect, Context]] = {
    import ref.c.universe.{Liftable, Quasiquote}

    val invoke = generateInvoke[C, Node, ActualFormat, Effect, Context, Api](ref)(method, format, system, api)
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
    ActualFormat <: MessageFormat[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    format: ref.c.Expr[ActualFormat],
    system: ref.c.Expr[EffectSystem[Effect]],
    api: ref.c.Expr[Api]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[(Seq[Node], Context) => Effect[Node]] = {
    import ref.c.universe.{weakTypeOf, Quasiquote}
    (weakTypeOf[Node], weakTypeOf[ActualFormat])

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

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
          if (argumentIndex == lastArgumentIndex && methodUsesContext[C, Context](ref)(method)) {
            q"context"
          } else {
            q"""
              (scala.util.Try($format.decode[${parameter.dataType}](argumentNodes($argumentIndex))) match {
                case scala.util.Failure(error) => scala.util.Failure(
                  automorph.protocol.ErrorType.InvalidRequestException("Invalid argument: " + $argumentIndex, error)
                )
                case result => result
              }).get
             """
          }
        }
      }

      // Create the API method call using the decoded arguments
      //   api.method(arguments ...): Effect[ResultValueType]
      val apiMethodCall = q"$api.${method.symbol}(...$arguments)"

      // Create encode result function
      //   (result: ResultValueType) => Node = format.encode[ResultValueType](result)
      val resultValueType = unwrapType[C, Effect[_]](ref.c)(method.resultType).dealias
      val encodeResult = q"(result: $resultValueType) => $format.encode[$resultValueType](result)"

      // Create the effect mapping call using the method call and the encode result function
      //   system.map(methodCall, encodeResult): Effect[Node]
      q"$system.map($apiMethodCall, $encodeResult)"
    }
    """)
  }

  private def logBoundMethod[C <: blackbox.Context, Api: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod,
    invoke: ref.c.Expr[Any]
  ): Unit = Option(System.getProperty(debugProperty)).foreach { _ =>
    println(
      s"""${methodSignature[C, Api](ref)(method)} =
        |  ${ref.c.universe.showCode(invoke.tree)}
        |""".stripMargin
    )
  }
}