package automorph.client

import automorph.client.ClientBinding
import automorph.protocol.MethodBindings.{call, methodSignature, methodToExpr, methodUsesContext, unwrapType, validApiMethods}
import automorph.spi.MessageFormat
import automorph.util.Reflection
import scala.quoted.{Expr, Quotes, Type}

/** JSON-RPC client layer bindings code generation. */
private[automorph] case object ClientBindings:

  private val debugProperty = "automorph.macro.debug"

  /**
   * Generates client bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @tparam Node message node type
   * @tparam ActualMessageFormat message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam Api API type
   * @return mapping of method names to client method bindings
   */
  inline def generate[Node, ActualMessageFormat <: MessageFormat[Node], Effect[_], Context, Api <: AnyRef](
    codec: ActualMessageFormat
  ): Map[String, ClientBinding[Node]] =
    ${ generateMacro[Node, ActualMessageFormat, Effect, Context, Api]('codec) }

  private def generateMacro[
    Node: Type,
    ActualMessageFormat <: MessageFormat[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api <: AnyRef: Type
  ](codec: Expr[ActualMessageFormat])(using quotes: Quotes): Expr[Map[String, ClientBinding[Node]]] =
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[Api, Effect](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors => ref.q.reflect.report.throwError(
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )

    // Generate bound API method bindings
    val clientMethods = Expr.ofSeq(validMethods.map { method =>
      '{
        ${ Expr(method.name) } -> ${
          generateBinding[Node, ActualMessageFormat, Effect, Context, Api](ref)(method, codec)
        }
      }
    })
    '{ $clientMethods.toMap }

  private def generateBinding[
    Node: Type,
    ActualMessageFormat <: MessageFormat[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api: Type
  ](ref: Reflection)(method: ref.RefMethod, codec: Expr[ActualMessageFormat]): Expr[ClientBinding[Node]] =
    given Quotes = ref.q

    val encodeArguments = generateEncodeArguments[Node, ActualMessageFormat, Context](ref)(method, codec)
    val decodeResult = generateDecodeResult[Node, ActualMessageFormat, Effect](ref)(method, codec)
    logBoundMethod[Api](ref)(method, encodeArguments, decodeResult)
    '{
      ClientBinding(
        ${ Expr(method.lift) },
        $encodeArguments,
        $decodeResult,
        ${ Expr(methodUsesContext[Context](ref)(method)) }
      )
    }

  private def generateEncodeArguments[Node: Type, ActualMessageFormat <: MessageFormat[Node]: Type, Context: Type](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[ActualMessageFormat]
  ): Expr[Seq[Any] => Seq[Node]] =
    import ref.q.reflect.{Term, asTerm}
    given Quotes = ref.q

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create encode arguments function
    //   (arguments: Seq[Any]) => Seq[Node]
    '{ arguments =>
      ${
        // Create the method argument lists by encoding corresponding argument values into nodes
        //   List(
        //     codec.encode[Parameter0Type](arguments(0).asInstanceOf[Parameter0Type]),
        //     codec.encode[Parameter1Type](arguments(1).asInstanceOf[Parameter1Type]),
        //     ...
        //     codec.encode[ParameterNType](arguments(N).asInstanceOf[ParameterNType])
        //   ): List[Node]
        val argumentNodes = method.parameters.toList.zip(parameterListOffsets).flatMap((parameters, offset) =>
          parameters.toList.zipWithIndex.flatMap { (parameter, index) =>
            Option.when((offset + index) != lastArgumentIndex || !methodUsesContext[Context](ref)(method)) {
              val argument = parameter.dataType.asType match
                case '[parameterType] => '{ arguments(${ Expr(offset + index) }).asInstanceOf[parameterType] }
              call(ref.q, codec.asTerm, "encode", List(parameter.dataType), List(List(argument.asTerm)))
            }
          }
        ).map(_.asInstanceOf[Term].asExprOf[Node])

        // Create the encoded arguments sequence construction call
        //   Seq(argumentNodes*): Seq[Node]
        '{ Seq(${ Expr.ofSeq(argumentNodes) }*) }
      }
    }

  private def generateDecodeResult[Node: Type, ActualMessageFormat <: MessageFormat[Node]: Type, Effect[_]: Type](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[ActualMessageFormat]
  ): Expr[Node => Any] =
    import ref.q.reflect.asTerm
    given Quotes = ref.q

    // Create decode result function
    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
    val resultValueType = unwrapType[Effect](ref.q)(method.resultType.dealias).dealias
    '{ resultNode =>
      ${
        call(ref.q, codec.asTerm, "decode", List(resultValueType), List(List('{ resultNode }.asTerm))).asExprOf[Any]
      }
    }

  private def logBoundMethod[Api: Type](ref: Reflection)(
    method: ref.RefMethod,
    encodeArguments: Expr[Any],
    decodeResult: Expr[Any]
  ): Unit =
    import ref.q.reflect.{Printer, asTerm}

    Option(System.getProperty(debugProperty)).foreach(_ =>
      println(
        s"""${methodSignature[Api](ref)(method)} =
          |  ${encodeArguments.asTerm.show(using Printer.TreeShortCode)}
          |  ${decodeResult.asTerm.show(using Printer.TreeShortCode)}
          |""".stripMargin
      )
    )
