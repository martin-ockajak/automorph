package jsonrpc.handler

import jsonrpc.spi.{Codec, Effect}
import jsonrpc.util.{Method, Reflection}
import scala.collection.immutable.ArraySeq
import scala.compiletime.error
import scala.quoted.{quotes, Expr, Quotes, Type}

/**
 * Bound API method handle.
 *
 * @param function binding function wrapping the bound method
 * @param name method name
 * @param resultType result type
 * @param paramNames parameter names
 * @param parameterTypes paramter types
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 * @tparam Context request context type
 */
final case class MethodHandle[Node, Outcome[_], Context](
  function: (Seq[Node], Option[Context]) => Outcome[Node],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String]
)

object HandlerMacros:

  /**
   * Generates JSON-RPC bindings for all valid public methods of an API type.
   *
   * Throws an exception if an invalid public method is found.
   * Methods are considered invalid if they satisfy one of these conditions:
   * * have type parameters
   * * cannot be called at runtime
   *
   * @param codec data format codec
   * @param effect effect system
   * @param api API instance
   * @tparam Node data format node representation type
   * @tparam CodecType data format codec type
   * @tparam Outcome computation outcome effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to their JSON-RPC wrapper functions
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Node, CodecType <: Codec[Node], Outcome[_], Context, ApiType <: AnyRef](
    codec: CodecType,
    effect: Effect[Outcome],
    api: ApiType
  ): Map[String, MethodHandle[Node, Outcome, Context]] = ${ bind('codec, 'effect, 'api) }

  private def bind[Node: Type, CodecType <: Codec[Node]: Type, Outcome[_]: Type, Context: Type, ApiType <: AnyRef: Type](
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]],
    api: Expr[ApiType]
  )(using quotes: Quotes): Expr[Map[String, MethodHandle[Node, Outcome, Context]]] =
    import ref.quotes.reflect.{asTerm, TypeRepr, TypeTree}
    val ref = Reflection(quotes)

    val decodeCall =
      callTerm(ref.quotes, codec.asTerm, "decode", List(TypeRepr.of[String]), List(List(Expr("TEST").asTerm))).asExpr
    //    '{
    //      $codec.decode[String](().asInstanceOf[Node])
    //    }

    println("--- CODEC TYPE ---")
    println(TypeTree.of[CodecType].show)
    println()

//    def methodArgument[T: Type](node: Expr[Node], param: ref.QuotedParameter): Expr[T] =
//      '{
//        $codec.decode[T]($node)
//      }

//    def methodFunction(method: ref.QuoteDMethod): Expr[Node => Outcome[Node]] =
//      '{
//      }

    // Detect and validate public methods in the API type
    val apiMethods = detectApiMethods(ref, TypeTree.of[ApiType])

    // Generate API method handles including wrapper functions consuming and product Node values
    val methodHandles = Expr.ofSeq(apiMethods.map { method =>
      generateMethodHandle[Node, CodecType, Outcome, Context, ApiType](ref, method, codec, effect, api)
    })

    // Generate function call using a type parameter
    val methodName = apiMethods.find(_.parameters.flatten.isEmpty).map(_.name).getOrElse("")
    val call = callTerm(ref.quotes, api.asTerm, methodName, List.empty, List.empty)
    val typeParameter = TypeRepr.of[List[List[String]]]
    val typedCall = callTerm(ref.quotes, '{ List }.asTerm, "apply", List(typeParameter), List.empty)

    // Debug prints
//    println(apiMethods.map(_.lift).map(methodDescription).mkString("\n"))
//    println(
//      s"""
//        |Call:
//        |  $call
//        |
//        |Typed call:
//        |  $typedCall
//        |""".stripMargin
//    )

    // Generate printouts code using the previously generated code
    '{
//      println(${call.asExpr})
//      println(${typedCall.asExpr})
      println("--- DECODED ---")
      println($decodeCall)
      $methodHandles.toMap[String, MethodHandle[Node, Outcome, Context]]
    }

  private def detectApiMethods(ref: Reflection, apiTypeTree: ref.quotes.reflect.TypeTree): Seq[ref.QuotedMethod] =
    import ref.quotes.reflect.{TypeRepr, TypeTree}

    given Quotes = ref.quotes
    val baseMethodNames = Seq(TypeRepr.of[AnyRef], TypeRepr.of[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val methods = ref.methods(apiTypeTree.tpe).filter(_.public).filter {
      method => !baseMethodNames.contains(method.symbol.name)
    }
    methods.foreach { method =>
      val signature = s"${apiTypeTree.show}.${method.lift.signature}"
      if method.typeParameters.nonEmpty then
        sys.error(s"Bound API method must not have type parameters: $signature")
      else if !method.available then
        sys.error(s"Bound API method must be callable at runtime: $signature")
    }
    methods

  private def generateMethodHandle[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Outcome[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]],
    api: Expr[ApiType]
  ): Expr[(String, MethodHandle[Node, Outcome, Context])] =
    given Quotes = ref.quotes
    val liftedMethod = method.lift
    val function = generateFunction[Node, CodecType, Outcome, Context, ApiType](ref, method, codec, effect, api)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val parameterNames = Expr(liftedMethod.parameters.flatMap(_.map(_.name)))
    val parameterTypes = Expr(liftedMethod.parameters.flatMap(_.map(_.dataType)))
    '{
      $name -> MethodHandle($function, $name, $resultType, $parameterNames, $parameterTypes)
    }

  private def generateFunction[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Outcome[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]],
    api: Expr[ApiType]
  ): Expr[(Seq[Node], Option[Context]) => Outcome[Node]] =
    import ref.quotes.reflect.{asTerm, Lambda, MethodType, Printer, Symbol, Term, Tree, TypeRepr}
    given Quotes = ref.quotes

    val argumentConverters = method.parameters.flatMap(_.map { parameter =>
      val parameterTypes = List(List(TypeRepr.of[Node]))
      lambdaExpr(ref.quotes, codec, "decode", List(parameter.dataType), parameterTypes, parameter.dataType)
    })
    val resultConverter =
      lambdaExpr(ref.quotes, codec, "encode", List(method.resultType), List(List(method.resultType)), TypeRepr.of[Node])
    val parameterTypes = method.parameters.map(params => List.fill(params.size)(TypeRepr.of[Any])).toList
    val methodCaller = lambdaExpr(ref.quotes, api, method.name, List.empty, parameterTypes, method.resultType)

    // Debug prints
    println(method.name)
    argumentConverters.foreach { converter =>
      println(s"  ${converter.asTerm.show(using Printer.TreeCode)}")
    }
    println(s"  ${resultConverter.asTerm.show(using Printer.TreeCode)}")
    println(methodCaller.asTerm.show(using Printer.TreeCode))
    println()

    val function = '{
      (nodeArguments: Seq[Node], context: Option[Context]) =>
        val arguments = nodeArguments.zip(${ Expr.ofSeq(argumentConverters) }).map { (argument, converter) =>
          converter.asInstanceOf[Node => Any](argument)
        }
        val convertResult = $resultConverter.asInstanceOf[Any => Node]
//        val callMethod = $methodCaller.asInstanceOf[Any => Outcome[Any]]
//        val outcome = callMethod(arguments)
//        $effect.map(outcome, convertResult)
        $effect.pure(nodeArguments.head)
    }
    println(function.asTerm.show(using Printer.TreeCode))
    function

  private def lambdaExpr(
    quotes: Quotes,
    instance: Expr[?],
    methodName: String,
    typeArguments: List[quotes.reflect.TypeRepr],
    parameterTypes: List[List[quotes.reflect.TypeRepr]],
    resultType: quotes.reflect.TypeRepr
  ): Expr[Any] =
    import quotes.reflect.{asTerm, Lambda, MethodType, Symbol, Term}
    val paramNames = parameterTypes.flatten.indices.map(index => s"p$index").toList
    val methodType = MethodType(paramNames)(_ => parameterTypes.flatten, _ => resultType)
    val paramIndices = parameterTypes.foldLeft(Seq(0))((indices, params) => indices :+ (indices.last + params.size))
    Lambda(
      Symbol.spliceOwner,
      methodType,
      (symbol, arguments) =>
        val argumentLists = paramIndices.zip(paramIndices.tail).map(arguments.slice).asInstanceOf[List[List[Term]]]
        callTerm(quotes, instance.asTerm, methodName, typeArguments, argumentLists)
    ).asExpr

  /**
   * Create instance method call term.
   *
   * @param quotes quototation context
   * @param instance instance term
   * @param methodName method name
   * @param typeArguments method type argument types
   * @param arguments method argument terms
   * @return instance method call term
   */
  private def callTerm(
    quotes: Quotes,
    instance: quotes.reflect.Term,
    methodName: String,
    typeArguments: List[quotes.reflect.TypeRepr],
    arguments: List[List[quotes.reflect.Term]]
  ): quotes.reflect.Term =
    import quotes.reflect.Select
    Select.unique(instance, methodName).appliedToTypes(typeArguments).appliedToArgss(arguments)
