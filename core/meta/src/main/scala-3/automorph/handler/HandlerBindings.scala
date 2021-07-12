package automorph.handler

import automorph.protocol.MethodBindings.{call, methodSignature, methodToExpr, methodUsesContext, unwrapType, validApiMethods}
import automorph.protocol.ErrorType.InvalidRequestException
import automorph.spi.{Backend, Codec}
import automorph.util.{Method, Reflection}
import scala.quoted.{Expr, Quotes, Type}
import scala.util.Try
import scala.util.Success
import scala.util.Failure

/** JSON-RPC handler layer bindings code generation. */
private[automorph] case object HandlerBindings:

  private val debugProperty = "automorph.macro.debug"

  /**
   * Generates handler bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @param backend effect system plugin
   * @param api API instance
   * @tparam Node message node type
   * @tparam ExactCodec message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam Api API type
   * @return mapping of method names to handler method bindings
   */
  inline def generate[Node, ExactCodec <: Codec[Node], Effect[_], Context, Api <: AnyRef](
    codec: ExactCodec,
    backend: Backend[Effect],
    api: Api
  ): Map[String, HandlerBinding[Node, Effect, Context]] =
    ${ generateMacro[Node, ExactCodec, Effect, Context, Api]('codec, 'backend, 'api) }

  private def generateMacro[
    Node: Type,
    ExactCodec <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api <: AnyRef: Type
  ](
    codec: Expr[ExactCodec],
    backend: Expr[Backend[Effect]],
    api: Expr[Api]
  )(using quotes: Quotes): Expr[Map[String, HandlerBinding[Node, Effect, Context]]] =
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[Api, Effect](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors => ref.q.reflect.report.throwError(
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )

    // Generate bound API method bindings
    val handlerBindings = Expr.ofSeq(validMethods.map { method =>
      '{
        ${ Expr(method.name) } -> ${
          generateBinding[Node, ExactCodec, Effect, Context, Api](ref)(method, codec, backend, api)
        }
      }
    })
    '{ $handlerBindings.toMap }

  private def generateBinding[
    Node: Type,
    ExactCodec <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api: Type
  ](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[ExactCodec],
    backend: Expr[Backend[Effect]],
    api: Expr[Api]
  ): Expr[HandlerBinding[Node, Effect, Context]] =
    given Quotes = ref.q

    val invoke = generateInvoke[Node, ExactCodec, Effect, Context, Api](ref)(method, codec, backend, api)
    logBoundMethod[Api](ref)(method, invoke)
    '{
      HandlerBinding(
        ${ Expr(method.lift) },
        $invoke,
        ${ Expr(methodUsesContext[Context](ref)(method)) }
      )
    }

  private def generateInvoke[
    Node: Type,
    ExactCodec <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api: Type
  ](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[ExactCodec],
    backend: Expr[Backend[Effect]],
    api: Expr[Api]
  ): Expr[(Seq[Node], Context) => Effect[Node]] =
    import ref.q.reflect.{asTerm, Term, TypeRepr}
    given Quotes = ref.q

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create invoke function
    //   (argumentNodes: Seq[Node], context: Context) => Effect[Node]
    '{ (argumentNodes, context) =>
      ${
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
        val arguments = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentIndex = offset + index
            val argumentNode = '{ argumentNodes(${ Expr(argumentIndex) }) }
            if argumentIndex == lastArgumentIndex && methodUsesContext[Context](ref)(method) then
              'context.asTerm
            else
              val decodeArguments = List(List(argumentNode.asTerm))
              val decodeCall = call(ref.q, codec.asTerm, "decode", List(parameter.dataType), decodeArguments)
              parameter.dataType.asType match
                case '[argumentType] => '{
                    (Try(${ decodeCall.asExprOf[argumentType] }) match
                      case Failure(error) =>
                        Failure(InvalidRequestException("Invalid argument: " + ${ Expr(argumentIndex) }, error))
                      case result => result
                    ).get
                  }.asTerm
          }
        ).asInstanceOf[List[List[Term]]]

        // Create the API method call using the decoded arguments
        //   api.method(arguments*): Effect[ResultValueType]
        val apiMethodCall = call(ref.q, api.asTerm, method.name, List.empty, arguments)

        // Create encode result function
        //   (result: ResultValueType) => Node = codec.encode[ResultValueType](result)
        val resultValueType = unwrapType[Effect](ref.q)(method.resultType).dealias
        val encodeResult = resultValueType.asType match
          case '[resultType] => '{ (result: resultType) =>
              ${
                call(ref.q, codec.asTerm, "encode", List(resultValueType), List(List('{ result }.asTerm))).asExprOf[Node]
              }
            }

        // Create the effect mapping call using the method call and the encode result function
        //   backend.map(methodCall, encodeResult): Effect[Node]
        val mapArguments = List(List(apiMethodCall, encodeResult.asTerm))
        call(ref.q, backend.asTerm, "map", List(resultValueType, TypeRepr.of[Node]), mapArguments).asExprOf[Effect[Node]]
      }
    }

  private def logBoundMethod[Api: Type](ref: Reflection)(method: ref.RefMethod, invoke: Expr[Any]): Unit =
    import ref.q.reflect.{asTerm, Printer}

    Option(System.getProperty(debugProperty)).foreach(_ =>
      println(
        s"""${methodSignature[Api](ref)(method)} =
          |  ${invoke.asTerm.show(using Printer.TreeShortCode)}
          |""".stripMargin
      )
    )
