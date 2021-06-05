package jsonrpc.handler

import java.beans.IntrospectionException
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.core.ApiReflection
import jsonrpc.util.{Method, Reflection}
import scala.collection.immutable.ArraySeq
import scala.quoted.{quotes, Expr, Quotes, Type}

/**
 * Bound API method handle.
 *
 * @param function binding function wrapping the bound method
 * @param name method name
 * @param resultType result type
 * @param paramNames parameter names
 * @param parameterTypes paramter types
 * @tparam Node message format node representation type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class MethodHandle[Node, Effect[_], Context](
  function: (Seq[Node], Context) => Effect[Node],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String],
  usesContext: Boolean
)

case object HandlerMacros:

  /**
   * Generate JSON-RPC bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @param backend effect backend plugin
   * @param api API instance
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to their JSON-RPC wrapper functions
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType,
    backend: Backend[Effect],
    api: ApiType
  ): Map[String, MethodHandle[Node, Effect, Context]] =
    ${ bind[Node, CodecType, Effect, Context, ApiType]('codec, 'backend, 'api) }

  private def bind[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type, Context: Type, ApiType <: AnyRef: Type](
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  )(using quotes: Quotes): Expr[Map[String, MethodHandle[Node, Effect, Context]]] =
    import ref.quotes.reflect.{asTerm, TypeRepr, TypeTree}
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = ApiReflection.detectApiMethods[Effect, Context](ref, TypeTree.of[ApiType])
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if invalidMethodErrors.nonEmpty then
      throw IntrospectionException(
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )

    // Debug prints
//    println(validMethods.map(_.lift).map(methodDescription).mkString("\n"))

    // Generate API method handles including wrapper functions consuming and product Node values
    val methodHandles = Expr.ofSeq(validMethods.map { method =>
      generateMethodHandle[Node, CodecType, Effect, Context, ApiType](ref, method, codec, backend, api)
    })
    '{
      $methodHandles.toMap[String, MethodHandle[Node, Effect, Context]]
    }

  private def generateMethodHandle[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  ): Expr[(String, MethodHandle[Node, Effect, Context])] =
    given Quotes = ref.quotes

    val liftedMethod = method.lift
    val function = generateBindingFunction[Node, CodecType, Effect, Context, ApiType](ref, method, codec, backend, api)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val parameterNames = Expr(liftedMethod.parameters.flatMap(_.map(_.name)))
    val parameterTypes = Expr(liftedMethod.parameters.flatMap(_.map(_.dataType)))
    val usesContext = Expr(methodUsesContext[Context](ref, method))
    '{
      $name -> MethodHandle($function, $name, $resultType, $parameterNames, $parameterTypes, $usesContext)
    }

  private def generateBindingFunction[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  ): Expr[(Seq[Node], Context) => Effect[Node]] =
    import ref.quotes.reflect.{asTerm, IntConstant, Lambda, Literal, MethodType, Printer, Symbol, Term, Tree, TypeRepr}
    given Quotes = ref.quotes

    // Method call function expression consuming argument nodes and returning the method call result
    val decodeArgumentsAndCallMethod =
      decodeArgumentsAndCallMethodExpr[Node, CodecType, Effect, Context, ApiType](ref, method, codec, backend, api)

    // Result conversion function expression consuming the method result and returning a node
    val encodeResult = encodeResultExpr[Node, CodecType](ref, method, codec)

    // Binding function expression
    val bindingFunction = '{
      (argumentNodes: Seq[Node], context: Context) =>
        $backend.map(
          $decodeArgumentsAndCallMethod(argumentNodes, context),
          $encodeResult.asInstanceOf[Any => Node]
        )
//        $decodeAndCallMethod(argumentNodes, context)
    }

    // Debug prints
//    println(method.name)
//    println(s"  ${methodCaller.asTerm.show(using Printer.TreeCode)}")
//    println(s"  ${resultConverter.asTerm.show(using Printer.TreeCode)}")
//    println(bindingFunction.asTerm.show(using Printer.TreeCode))
//    println()
    bindingFunction

  private def decodeArgumentsAndCallMethodExpr[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  ): Expr[(Seq[Node], Context) => Effect[Any]] =
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
            callTerm(ref.quotes, codec.asTerm, "decode", List(parameter.dataType), List(List(argumentNode)))
          }
        )
        val contextArgumentLists = if methodUsesContext[Context](ref, method) then List(List(arguments.last)) else List.empty
        val allArgumentLists = (argumentLists ++ contextArgumentLists).asInstanceOf[List[List[Term]]]

        // Call the method using the decoded arguments
        callTerm(ref.quotes, api.asTerm, method.name, List.empty, allArgumentLists)
//        val methodCall = callTerm(ref.quotes, api.asTerm, method.name, List.empty, argumentLists)
//
//        // Encode the method call result into a node
//        val encodeResult = encodeResultExpr[Node, CodecType](ref, method, codec)
//        callTerm(ref.quotes, backend.asTerm, "map", List(method.resultType, TypeRepr.of[Node]), List(List(methodCall, convertResult.asTerm)))
    ).asExpr.asInstanceOf[Expr[(Seq[Node], Context) => Effect[Any]]]

  private def encodeResultExpr[Node: Type, CodecType <: Codec[Node]: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[Any => Node] =
    import ref.quotes.reflect.{asTerm, AppliedType, Lambda, MethodType, Symbol, Term, TypeRepr}
    given Quotes = ref.quotes

    val resultType =
      method.resultType match
        case appliedType: AppliedType => appliedType.args.last
        case otherType                        => otherType
    Lambda(
      Symbol.spliceOwner,
      MethodType(List("result"))(_ => List(resultType), _ => TypeRepr.of[Node]),
      (symbol, arguments) =>
        callTerm(ref.quotes, codec.asTerm, "encode", List(resultType), List(arguments.asInstanceOf[List[Term]]))
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

  private def methodUsesContext[Context: Type](ref: Reflection, method: ref.QuotedMethod): Boolean =
    import ref.quotes.reflect.TypeRepr

    method.parameters.flatten.lastOption.exists(_.dataType =:= TypeRepr.of[Context])

  private def methodDescription(method: Method): String =
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.signature}\n"
