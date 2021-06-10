package jsonrpc.client

import jsonrpc.client.ClientMethod
import jsonrpc.core.MethodBindings.{effectResultType, methodCall, methodDescription, methodUsesContext, validApiMethods}
import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.quoted.{Expr, Quotes, Type}

/** JSON-RPC client layer bindings code generation. */
case object ClientBindings:

  private val debugProperty = "jsonrpc.macro.debug"
//  private val debugDefault = "true"
  private val debugDefault = ""

//  inline def generateX[Node, CodecType <: Codec[Node], Arguments <: Tuple]( codec: CodecType ): Tuple => Seq[Node] =
//    ${ generateX[Node, CodecType, Effect, Context, ApiType]('codec) }
//
//  private def generateX[Node: Type, CodecType <: Codec[Node]: Type, Arguments <: Tuple: Type](
//    codec: Expr[CodecType]
//  )(using quotes: Quotes): Expr[Map[String, ClientMethod[Node]]] =
//    import quotes.reflect.{asTerm, AppliedType, Lambda, MethodType, Symbol, Term, TypeRepr}
//
//    // Map multiple parameter lists to flat argument node list offsets
//    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
//      indices :+ (indices.last + size)
//    }
//
//    // Create encode arguments function
//    //   (arguments: Seq[Any]) => Seq[Node]
//    val encodeArgumentsType = MethodType(List("arguments"))(
//      _ => List(TypeRepr.of[Tuple]),
//      _ => TypeRepr.of[Seq[Node]]
//    )
//    Lambda(
//      Symbol.spliceOwner,
//      encodeArgumentsType,
//      (symbol, arguments) =>
//        // Create the method argument lists by encoding corresponding argument values into nodes
//        //   List(
//        //     codec.encode[Parameter0Type](arguments(0).asInstanceOf[Parameter0Type]),
//        //     codec.encode[Parameter1Type](arguments(1).asInstanceOf[Parameter1Type]),
//        //     ...
//        //     codec.encode[ParameterNType](arguments(N).asInstanceOf[ParameterNType])
//        //   )): List[Node]
//        val List(argumentValues) = arguments.asInstanceOf[List[Term]]
//        val argumentList = Expr.ofSeq(argumentValues.zipWithIndex.map { (parameter, index) =>
//          val argumentIndex = Expr(index)
//          val argument = parameter.dataType.asType match
//            case '[parameterType] => '{ ${ argumentValues.asExprOf[Seq[Any]] }($argumentIndex) }
//          val argument = methodCall(quotes, argumentValues, s"_${argumentIndex}", List(), List())
//          methodCall(quotes, codec.asTerm, "encode", List(parameter.dataType), List(List(argument.asTerm)))
//        }.map(_.asInstanceOf[Term].asExprOf[Node]))
//
//        // Create the encoded arguments sequence construction call
//        //   Seq(encodedArguments ...): Seq[Node]
//        '{ Seq(${ argumentList }*) }.asTerm
//    ).asExprOf[Tuple => Seq[Node]]
//    Expr(()).asInstanceOf[Expr[Tuple => Seq[Node]]]

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
  ](
    codec: Expr[CodecType]
  )(using quotes: Quotes): Expr[Map[String, ClientMethod[Node]]] =
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[Effect](ref, ref.quotes.reflect.TypeTree.of[ApiType])
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if invalidMethodErrors.nonEmpty then
      ref.quotes.reflect.report.throwError(
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )

    // Generate bound API method bindings
    val clientMethods = Expr.ofSeq(validMethods.map { method =>
      generateClientMethod[Node, CodecType, Effect, Context, ApiType](ref, method, codec)
    })
    '{ $clientMethods.toMap[String, ClientMethod[Node]] }

  private def generateClientMethod[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[(String, ClientMethod[Node])] =
    given Quotes = ref.quotes

    val liftedMethod = method.lift
    val encodeArguments = generateEncodeArgumentsFunction[Node, CodecType, Context](ref, method, codec)
    val decodeResult = generateDecodeResultFunction[Node, CodecType, Effect](ref, method, codec)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val parameterNames = Expr(liftedMethod.parameters.flatMap(_.map(_.name)))
    val parameterTypes = Expr(liftedMethod.parameters.flatMap(_.map(_.dataType)))
    val usesContext = Expr(methodUsesContext[Context](ref, method))
    logBoundMethod[ApiType](ref, method, encodeArguments, decodeResult)
    '{
      $name -> ClientMethod(
        $encodeArguments,
        $decodeResult,
        $name,
        $resultType,
        $parameterNames,
        $parameterTypes,
        $usesContext
      )
    }

  private def generateEncodeArgumentsFunction[Node: Type, CodecType <: Codec[Node]: Type, Context: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[Seq[Any] => Seq[Node]] =
    import ref.quotes.reflect.{asTerm, AppliedType, Lambda, MethodType, Symbol, Term, TypeRepr}
    given Quotes = ref.quotes

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }

    // Create encode arguments function
    //   (arguments: Seq[Any]) => Seq[Node]
    val encodeArgumentsType = MethodType(List("arguments"))(
      _ => List(TypeRepr.of[Seq[Any]]),
      _ => TypeRepr.of[Seq[Node]]
    )
    Lambda(
      Symbol.spliceOwner,
      encodeArgumentsType,
      (symbol, arguments) =>
        // Create the method argument lists by encoding corresponding argument values into nodes
        //   List(
        //     codec.encode[Parameter0Type](arguments(0).asInstanceOf[Parameter0Type]),
        //     codec.encode[Parameter1Type](arguments(1).asInstanceOf[Parameter1Type]),
        //     ...
        //     codec.encode[ParameterNType](arguments(N).asInstanceOf[ParameterNType])
        //   )): List[Node]
        val List(argumentValues) = arguments.asInstanceOf[List[Term]]
        val parameters = method.parameters.dropRight(if methodUsesContext[Context](ref, method) then 1 else 0)
        val argumentList = Expr.ofSeq(parameters.toList.zip(parameterListOffsets).flatMap((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentIndex = Expr(offset + index)
            val argument = parameter.dataType.asType match
              case '[parameterType] =>
                '{ ${ argumentValues.asExprOf[Seq[Any]] }($argumentIndex).asInstanceOf[parameterType] }
            methodCall(ref.quotes, codec.asTerm, "encode", List(parameter.dataType), List(List(argument.asTerm)))
          }
        ).map(_.asInstanceOf[Term].asExprOf[Node]))

        // Create the encoded arguments sequence construction call
        //   Seq(encodedArguments ...): Seq[Node]
        '{ Seq(${ argumentList }*) }.asTerm
    ).asExprOf[Seq[Any] => Seq[Node]]

  private def generateDecodeResultFunction[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[Node => Any] =
    import ref.quotes.reflect.{asTerm, AppliedType, IntConstant, Lambda, Literal, MethodType, Symbol, Term, TypeRepr}
    given Quotes = ref.quotes

    // Create decode result function
    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
    val resultValueType = effectResultType[Effect](ref, method)
    val decodeResultType = MethodType(List("resultNode"))(_ => List(TypeRepr.of[Node]), _ => resultValueType)
    Lambda(
      Symbol.spliceOwner,
      decodeResultType,
      (symbol, arguments) => methodCall(ref.quotes, codec.asTerm, "decode", List(resultValueType), List(arguments))
    ).asExprOf[Node => Any]

  private def logBoundMethod[ApiType: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    encodeArguments: Expr[Any],
    decodeResult: Expr[Any]
  ): Unit =
    import ref.quotes.reflect.{asTerm, Printer, TypeRepr}

    if Option(System.getenv(debugProperty)).getOrElse(debugDefault).nonEmpty then
      println(
        s"${methodDescription[ApiType](ref, method)} = \n  ${encodeArguments.asTerm.show(using
          Printer.TreeAnsiCode
        )}\n  ${decodeResult.asTerm.show(using Printer.TreeAnsiCode)}\n"
      )
