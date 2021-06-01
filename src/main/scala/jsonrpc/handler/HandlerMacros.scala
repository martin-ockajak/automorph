package jsonrpc.handler

import jsonrpc.spi.{Codec, Effect}
import jsonrpc.util.{Method, Reflection}
import java.beans.IntrospectionException
import jsonrpc.util.ValueOps.{asLeft, asRight}
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
  function: (Seq[Node], Context) => Outcome[Node],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String]
)

object HandlerMacros:

  /**
   * Generate JSON-RPC bindings for all valid public methods of an API type.
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

    // Detect and validate public methods in the API type
    val apiMethods = detectApiMethods[Outcome, Context](ref, TypeTree.of[ApiType])
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethods = apiMethods.flatMap(_.swap.toOption)
    if invalidMethods.nonEmpty then
      throw new IntrospectionException(s"Failed to bind API methods:\n${invalidMethods.map((_, error) => s"  $error").mkString("\n")}")

    // Debug prints
    println(validMethods.map(_.lift).map(methodDescription).mkString("\n"))

    // Generate API method handles including wrapper functions consuming and product Node values
    val methodHandles = Expr.ofSeq(validMethods.map { method =>
      generateMethodHandle[Node, CodecType, Outcome, Context, ApiType](ref, method, codec, effect, api)
    })

    // Generate printouts code using the previously generated code
    '{
      $methodHandles.toMap[String, MethodHandle[Node, Outcome, Context]]
    }

  private def detectApiMethods[Outcome[_]: Type, Context: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree
  ): Seq[Either[(String, String), ref.QuotedMethod]] =
    import ref.quotes.reflect.{TypeRepr, TypeTree}
    given Quotes = ref.quotes

    val baseMethodNames = Seq(TypeRepr.of[AnyRef], TypeRepr.of[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val methods = ref.methods(apiType.tpe).filter(_.public).filter {
      method => !baseMethodNames.contains(method.symbol.name)
    }
    methods.map(method => validateApiMethod(ref, apiType, method))

  private def validateApiMethod[Outcome[_]: Type, Context: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree,
    method: ref.QuotedMethod
  ): Either[(String, String), ref.QuotedMethod] =
    import ref.quotes.reflect.{AppliedType, LambdaType, NamedType, TypeRepr}

    val signature = s"${apiType.show}.${method.lift.signature}"
    if method.typeParameters.nonEmpty then
      (method.name -> s"Bound API method '$signature' must not have type parameters").asLeft
    else if !method.available then
      (method.name -> s"Bound API method '$signature' must be callable at runtime").asLeft
    else if contextSupplied[Context](ref.quotes) && method.parameters.lastOption.map { parameters =>
        !(parameters.last.dataType =:= TypeRepr.of[Context])
      }.getOrElse(true)
    then
      (method.name -> s"Bound API method '$signature' must accept the specified request context type '${TypeRepr.of[Context].show}' as its last parameter").asLeft
    else
      TypeRepr.of[Outcome] match
        case lambdaType: LambdaType =>
          if method.resultType match
              case appliedType: AppliedType => !(appliedType.tycon =:= lambdaType)
              case _                        => false
          then
            (method.name -> s"Bound API method '$signature' must return the specified effect type '${lambdaType.resType.show}'").asLeft
          else
            method.asRight
        case _ => method.asRight

  private def contextSupplied[Context: Type](quotes: Quotes): Boolean =
    import quotes.reflect.TypeRepr
    given Quotes = quotes

    !(TypeRepr.of[Context] =:= TypeRepr.of[Unit])

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
    val function = generateBindingFunction[Node, CodecType, Outcome, Context, ApiType](ref, method, codec, effect, api)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val parameterNames = Expr(liftedMethod.parameters.flatMap(_.map(_.name)))
    val parameterTypes = Expr(liftedMethod.parameters.flatMap(_.map(_.dataType)))
    '{
      $name -> MethodHandle($function, $name, $resultType, $parameterNames, $parameterTypes)
    }

  private def generateBindingFunction[
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
  ): Expr[(Seq[Node], Context) => Outcome[Node]] =
    import ref.quotes.reflect.{asTerm, IntConstant, Lambda, Literal, MethodType, Printer, Symbol, Term, Tree, TypeRepr}
    given Quotes = ref.quotes

    // Method call function expression consuming argument nodes and returning the method call result
    val decodeAndCallMethod =
      decodeAndCallMethodExpr[Node, CodecType, Outcome, Context, ApiType](ref, method, codec, effect, api)

    // Result conversion function expression consuming the method result and returning a node
    val encodeResult = encodeResultExpr[Node, CodecType](ref, method, codec)

    // Binding function expression
    val bindingFunction = '{
      (argumentNodes: Seq[Node], context: Context) =>
        val outcome = $decodeAndCallMethod(argumentNodes, context)
        $effect.map(outcome, $encodeResult.asInstanceOf[Any => Node])
//        val decodeAndCallMethod = $methodCaller.asInstanceOf[Seq[Node] => Outcome[Node]]
//        decodeAndCallMethod(argumentNodes)
    }

    // Debug prints
//    println(method.name)
//    println(s"  ${methodCaller.asTerm.show(using Printer.TreeCode)}")
//    println(s"  ${resultConverter.asTerm.show(using Printer.TreeCode)}")
    println(bindingFunction.asTerm.show(using Printer.TreeCode))
    println()
    bindingFunction

  private def decodeAndCallMethodExpr[
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
  ): Expr[(Seq[Node], Context) => Outcome[Any]] =
    import ref.quotes.reflect.{asTerm, IntConstant, Lambda, Literal, MethodType, Symbol, Term, TypeRepr}
    given Quotes = ref.quotes

    Lambda(
      Symbol.spliceOwner,
      MethodType(List("argumentNodes", "context"))(
        _ => List(TypeRepr.of[Seq[Node]], TypeRepr.of[Context]),
        _ => method.resultType
      ),
      (symbol, arguments) =>
        // Map multiple parameter lists to flat argument node list offsets
        val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
          indices :+ (indices.last + size)
        }
        val lastArgumentIndex = method.parameters.map(_.size).sum - 1

        // Create method argument lists by decoding corresponding argument nodes into required types
        val argumentLists = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentNodes = arguments.head.asInstanceOf[Term]
            val argumentIndex = Literal(IntConstant(offset + index))
            val argumentNode = callTerm(ref.quotes, argumentNodes, "apply", List.empty, List(List(argumentIndex)))
            if contextSupplied[Context](ref.quotes) && (offset + index) == lastArgumentIndex then
              arguments.last.asInstanceOf[Term]
            else
              callTerm(ref.quotes, codec.asTerm, "decode", List(parameter.dataType), List(List(argumentNode)))
          }
        ).asInstanceOf[List[List[Term]]]

        // Call the method using the decoded arguments
        callTerm(ref.quotes, api.asTerm, method.name, List.empty, argumentLists)
//        val methodCall = callTerm(ref.quotes, api.asTerm, method.name, List.empty, argumentLists)
//
//        // Encode the method call result into a node
//        val convertResult = convertResultExpr[Node, CodecType](ref, method, codec)
//        callTerm(ref.quotes, effect.asTerm, "map", List(method.resultType, TypeRepr.of[Node]), List(List(methodCall, convertResult.asTerm)))
    ).asExpr.asInstanceOf[Expr[(Seq[Node], Context) => Outcome[Any]]]

  private def encodeResultExpr[Node: Type, CodecType <: Codec[Node]: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[Any => Node] =
    import ref.quotes.reflect.{asTerm, Lambda, MethodType, Symbol, Term, TypeRepr}

    Lambda(
      Symbol.spliceOwner,
      MethodType(List("result"))(_ => List(method.resultType), _ => TypeRepr.of[Node]),
      (symbol, arguments) =>
        callTerm(ref.quotes, codec.asTerm, "encode", List(method.resultType), List(arguments.asInstanceOf[List[Term]]))
    ).asExpr.asInstanceOf[Expr[Any => Node]]

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
    quotes.reflect.Select.unique(instance, methodName).appliedToTypes(typeArguments).appliedToArgss(arguments)

  private def methodDescription(method: Method): String =
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.signature}\n"
