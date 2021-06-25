package jsonrpc.handler

import jsonrpc.protocol.MethodBindings.{call, methodSignature, methodUsesContext, unwrapType, validApiMethods}
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.util.Reflection
import scala.quoted.{Expr, Quotes, Type}

/** JSON-RPC handler layer bindings code generation. */
private[jsonrpc] case object HandlerBindings:

  private val debugProperty = "jsonrpc.macro.debug"

  /**
   * Generate handler bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @param backend effect backend plugin
   * @param api API instance
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to handler method bindings
   */
  inline def generate[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType,
    backend: Backend[Effect],
    api: ApiType
  ): Map[String, HandlerMethod[Node, Effect, Context]] =
    ${ generate[Node, CodecType, Effect, Context, ApiType]('codec, 'backend, 'api) }

  private def generate[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType <: AnyRef: Type
  ](
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  )(using quotes: Quotes): Expr[Map[String, HandlerMethod[Node, Effect, Context]]] =
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[ApiType, Effect](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors => ref.q.reflect.report.throwError(
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )

    // Generate bound API method bindings
    val handlerMethods = Expr.ofSeq(validMethods.map { method =>
      '{
        ${ Expr(method.name) } -> ${
          generateHandlerMethod[Node, CodecType, Effect, Context, ApiType](ref)(method, codec, backend, api)
        }
      }
    })
    '{ $handlerMethods.toMap }

  private def generateHandlerMethod[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  ): Expr[HandlerMethod[Node, Effect, Context]] =
    given Quotes = ref.q

    val invoke = generateInvoke[Node, CodecType, Effect, Context, ApiType](ref)(method, codec, backend, api)
    logBoundMethod[ApiType](ref)(method, invoke)
    '{
      HandlerMethod(
        $invoke,
        ${ Expr(method.lift.name) },
        ${ Expr(method.lift.resultType) },
        ${ Expr(method.lift.parameters.flatMap(_.map(_.name))) },
        ${ Expr(method.lift.parameters.flatMap(_.map(_.dataType))) },
        ${ Expr(methodUsesContext[Context](ref)(method)) }
      )
    }

  private def generateInvoke[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
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
        //     codec.decode[Parameter0Type](argumentNodes(0)),
        //     codec.decode[Parameter1Type](argumentNodes(1)),
        //     ...
        //     codec.decode[ParameterNType](argumentNodes(N)) OR context
        //   )): List[List[ParameterXType]]
        val arguments = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentNode = '{ argumentNodes(${ Expr(offset + index) }) }
            if (offset + index) == lastArgumentIndex && methodUsesContext[Context](ref)(method) then
              'context.asTerm
            else
              call(ref.q, codec.asTerm, "decode", List(parameter.dataType), List(List(argumentNode.asTerm)))
          }
        ).asInstanceOf[List[List[Term]]]

        // Create the API method call using the decoded arguments
        //   api.method(arguments*): Effect[ResultValueType]
        val apiMethodCall = call(ref.q, api.asTerm, method.name, List.empty, arguments)

        // Create encode result function
        //   (result: ResultValueType) => Node = codec.encode[ResultValueType](result)
        val resultValueType = unwrapType[Effect](ref.q)(method.resultType)
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

  private def logBoundMethod[ApiType: Type](ref: Reflection)(method: ref.RefMethod, invoke: Expr[Any]): Unit =
    import ref.q.reflect.{asTerm, Printer}

    Option(System.getProperty(debugProperty)).foreach( _ =>
      println(
        s"""${methodSignature[ApiType](ref)(method)} =
          |  ${invoke.asTerm.show(using Printer.TreeShortCode)}
          |""".stripMargin
      )
    )
