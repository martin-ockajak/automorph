package automorph.client.meta

import automorph.Contextual
import automorph.client.ClientBinding
import automorph.log.MacroLogger
import automorph.reflection.MethodReflection.functionToExpr
import automorph.reflection.{ClassReflection, MethodReflection}
import automorph.spi.MessageCodec
import scala.quoted.{Expr, Quotes, Type}

/** RPC client layer bindings code generation. */
private[automorph] object ClientGenerator:

  /**
   * Generates client bindings for all valid public methods of an API type.
   *
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context message context type
   * @tparam Api API type
   * @return mapping of API method names to client function bindings
   */
  inline def bindings[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    codec: Codec
  ): Seq[ClientBinding[Node, Context]] = ${ bindingsMacro[Node, Codec, Effect, Context, Api]('codec) }

  private def bindingsMacro[
    Node: Type,
    Codec <: MessageCodec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api <: AnyRef: Type
  ](codec: Expr[Codec])(using quotes: Quotes): Expr[Seq[ClientBinding[Node, Context]]] =
    val ref = ClassReflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = MethodReflection.apiMethods[Api, Effect](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors => ref.q.reflect.report.throwError(
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )

    // Generate bound API method bindings
    val clientBindings = validMethods.map { method =>
      generateBinding[Node, Codec, Effect, Context, Api](ref)(method, codec)
    }
    Expr.ofSeq(clientBindings)

  private def generateBinding[
    Node: Type,
    Codec <: MessageCodec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api: Type
  ](ref: ClassReflection)(method: ref.RefMethod, codec: Expr[Codec]): Expr[ClientBinding[Node, Context]] =
    given Quotes = ref.q

    val encodeArguments = generateEncodeArguments[Node, Codec, Context](ref)(method, codec)
    val decodeResult = generateDecodeResult[Node, Codec, Effect, Context](ref)(method, codec)
    logBoundMethod[Api](ref)(method, encodeArguments, decodeResult)
    '{
      ClientBinding(
        ${ Expr(method.lift.rpcFunction) },
        $encodeArguments,
        $decodeResult,
        ${ Expr(MethodReflection.acceptsContext[Context](ref)(method)) }
      )
    }

  private def generateEncodeArguments[Node: Type, Codec <: MessageCodec[Node]: Type, Context: Type](ref: ClassReflection)(
    method: ref.RefMethod,
    codec: Expr[Codec]
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
            Option.when((offset + index) != lastArgumentIndex || !MethodReflection.acceptsContext[Context](ref)(method)) {
              val argument = parameter.dataType.asType match
                case '[parameterType] => '{ arguments(${ Expr(offset + index) }).asInstanceOf[parameterType] }
              MethodReflection.call(
                ref.q,
                codec.asTerm,
                MessageCodec.encodeMethod,
                List(parameter.dataType),
                List(List(argument.asTerm))
              )
            }
          }
        ).map(_.asInstanceOf[Term].asExprOf[Node])

        // Create the encoded arguments sequence construction call
        //   Seq(argumentNodes*): Seq[Node]
        '{ Seq(${ Expr.ofSeq(argumentNodes) }*) }
      }
    }

  private def generateDecodeResult[Node: Type, Codec <: MessageCodec[Node]: Type, Effect[_]: Type, Context: Type](
    ref: ClassReflection
  )(method: ref.RefMethod, codec: Expr[Codec]): Expr[(Node, Context) => Any] =
    import ref.q.reflect.asTerm
    given Quotes = ref.q

    // Create decode result function
    //   (resultNode: Node, responseContext: Context) => ResultType = codec.decode[ResultType](resultNode)
    val resultType = MethodReflection.unwrapType[Effect](ref.q)(method.resultType).dealias
    MethodReflection.contextualResult[Context, Contextual](ref.q)(resultType).map { contextualResultType =>
      '{ (resultNode: Node, responseContext: Context) =>
        Contextual(
          ${
            MethodReflection.call(
              ref.q,
              codec.asTerm,
              MessageCodec.decodeMethod,
              List(contextualResultType),
              List(List('{ resultNode }.asTerm))
            ).asExprOf[Any]
          },
          responseContext
        )
      }
    }.getOrElse {
      '{ (resultNode: Node, _: Context) =>
        ${
          MethodReflection.call(
            ref.q,
            codec.asTerm,
            "decode",
            List(resultType),
            List(List('{ resultNode }.asTerm))
          ).asExprOf[Any]
        }
      }
    }

  private def logBoundMethod[Api: Type](ref: ClassReflection)(
    method: ref.RefMethod,
    encodeArguments: Expr[Any],
    decodeResult: Expr[Any]
  ): Unit =
    import ref.q.reflect.{Printer, asTerm}

    MacroLogger.debug(
      s"""${MethodReflection.signature[Api](ref)(method)} =
        |  ${encodeArguments.asTerm.show(using Printer.TreeShortCode)}
        |  ${decodeResult.asTerm.show(using Printer.TreeShortCode)}
        |""".stripMargin
    )
