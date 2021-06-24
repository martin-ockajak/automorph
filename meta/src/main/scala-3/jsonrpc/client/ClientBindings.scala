package jsonrpc.client

import jsonrpc.client.ClientMethod
import jsonrpc.protocol.MethodBindings.{call, methodSignature, methodUsesContext, unwrapType, validApiMethods}
import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.quoted.{Expr, Quotes, Type}

/** JSON-RPC client layer bindings code generation. */
private[jsonrpc] case object ClientBindings:

  private val debugProperty = "jsonrpc.macro.debug"
//  private val debugDefault = "true"
  private val debugDefault = ""

  /**
   * Generate client bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to client method bindings
   */
  inline def generate[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType
  ): Map[String, ClientMethod[Node]] =
    ${ generate[Node, CodecType, Effect, Context, ApiType]('codec) }

  private def generate[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType <: AnyRef: Type
  ](codec: Expr[CodecType])(using quotes: Quotes): Expr[Map[String, ClientMethod[Node]]] =
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[ApiType, Effect](ref)
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if invalidMethodErrors.nonEmpty then
      ref.q.reflect.report.throwError(
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )

    // Generate bound API method bindings
    val clientMethods = Expr.ofSeq(validMethods.map { method =>
      generateClientMethod[Node, CodecType, Effect, Context, ApiType](ref)(method, codec)
    })
    '{ $clientMethods.toMap[String, ClientMethod[Node]] }

  private def generateClientMethod[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](ref: Reflection)(method: ref.RefMethod, codec: Expr[CodecType]): Expr[(String, ClientMethod[Node])] =
    given Quotes = ref.q

    val encodeArguments = generateEncodeArguments[Node, CodecType, Context](ref)(method, codec)
    val decodeResult = generateDecodeResult[Node, CodecType, Effect](ref)(method, codec)
    logBoundMethod[ApiType](ref)(method, encodeArguments, decodeResult)
    '{
      ${ Expr(method.lift.name) } -> ClientMethod(
        $encodeArguments,
        $decodeResult,
        ${ Expr(method.lift.name) },
        ${ Expr(method.lift.resultType) },
        ${ Expr(method.lift.parameters.flatMap(_.map(_.name))) },
        ${ Expr(method.lift.parameters.flatMap(_.map(_.dataType))) },
        ${ Expr(methodUsesContext[Context](ref)(method)) }
      )
    }

  private def generateEncodeArguments[Node: Type, CodecType <: Codec[Node]: Type, Context: Type](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[CodecType]
  ): Expr[Seq[Any] => Seq[Node]] =
    import ref.q.reflect.{asTerm, Term}
    given Quotes = ref.q

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create encode arguments function
    //   (arguments: Seq[Any]) => Seq[Node]
    '{ (arguments: Seq[Any]) =>
      ${
        // Create the method argument lists by encoding corresponding argument values into nodes
        //   List(
        //     codec.encode[Parameter0Type](arguments(0).asInstanceOf[Parameter0Type]),
        //     codec.encode[Parameter1Type](arguments(1).asInstanceOf[Parameter1Type]),
        //     ...
        //     codec.encode[ParameterNType](arguments(N).asInstanceOf[ParameterNType])
        //   ): List[Node]
        val argumentList = method.parameters.toList.zip(parameterListOffsets).flatMap((parameters, offset) =>
          parameters.toList.zipWithIndex.flatMap { (parameter, index) =>
            Option.when((offset + index) != lastArgumentIndex || !methodUsesContext[Context](ref)(method)) {
              val argument = parameter.dataType.asType match
                case '[parameterType] => '{ arguments(${ Expr(offset + index) }).asInstanceOf[parameterType] }
              call(ref.q, codec.asTerm, "encode", List(parameter.dataType), List(List(argument.asTerm)))
            }
          }
        ).map(_.asInstanceOf[Term].asExprOf[Node])

        // Create the encoded arguments sequence construction call
        //   Seq(encodedArguments ...): Seq[Node]
        '{ Seq(${ Expr.ofSeq(argumentList) }*) }
      }
    }

  private def generateDecodeResult[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[CodecType]
  ): Expr[Node => Any] =
    import ref.q.reflect.asTerm
    given Quotes = ref.q

    // Create decode result function
    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
    val resultValueType = unwrapType[Effect](ref)(method.resultType)
    '{ (resultNode: Node) =>
      ${
        call(ref.q, codec.asTerm, "decode", List(resultValueType), List(List('{ resultNode }.asTerm))).asExprOf[Any]
      }
    }

  private def logBoundMethod[ApiType: Type](ref: Reflection)(
    method: ref.RefMethod,
    encodeArguments: Expr[Any],
    decodeResult: Expr[Any]
  ): Unit =
    import ref.q.reflect.{asTerm, Printer}

    if Option(System.getProperty(debugProperty)).getOrElse(debugDefault).nonEmpty then
      println(
        s"""${methodSignature[ApiType](ref)(method)} =
          |  ${encodeArguments.asTerm.show(using Printer.TreeShortCode)}
          |  ${decodeResult.asTerm.show(using Printer.TreeShortCode)}
          |  """.stripMargin
      )
