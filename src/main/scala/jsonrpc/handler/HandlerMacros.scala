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
 * @param paramTypes paramter types
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 * @tparam Context request context type
 */
final case class MethodHandle[Node, Outcome[_], Context](
  function: (Seq[Node], Option[Context]) => Outcome[Node],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  paramTypes: Seq[String]
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
   * @tparam ApiType API type
   * @tparam Node data format node representation type
   * @tparam Outcome computation outcome effect type
   * @tparam Context request context type
   * @return mapping of method names to their JSON-RPC wrapper functions
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[ApiType <: AnyRef, Node, Outcome[_], CodecType <: Codec[Node], Context](
    codec: CodecType,
    effect: Effect[Outcome],
    api: ApiType
  ): Map[String, MethodHandle[Node, Outcome, Context]] = ${ bind('codec, 'effect, 'api) }

  private def bind[ApiType <: AnyRef: Type, Node: Type, Outcome[_]: Type, CodecType <: Codec[Node]: Type, Context: Type](
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]],
    api: Expr[ApiType]
  )(using quotes: Quotes): Expr[Map[String, MethodHandle[Node, Outcome, Context]]] =
    import ref.quotes.reflect.{asTerm, TypeRepr, TypeTree}

    val ref = Reflection(quotes)

    val decodeCall =
      ref.callTerm(codec.asTerm, "decode", List(TypeRepr.of[String]), List(List(Expr("TEST").asTerm))).asExpr
    //    '{
    //      $codec.decode[String](().asInstanceOf[Node])
    //    }

    println("--- CODEC TYPE ---")
    println(TypeTree.of[CodecType].show)
    println()

//    def methodArgument[T: Type](node: Expr[Node], param: ref.QuotedParam): Expr[T] =
//      '{
//        $codec.decode[T]($node)
//      }

//    def methodFunction(method: ref.QuoteDMethod): Expr[Node => Outcome[Node]] =
//      '{
//      }

    // Detect and validate public methods in the API type
    val apiMethods = detectApiMethods(ref, TypeTree.of[ApiType])

    // Generate method handles including wrapper functions consuming and product Node values
    val methodHandles =
      Expr.ofSeq(apiMethods.map(method => generateMethodHandle[Node, Outcome, CodecType, Context](ref, method, codec, effect)))
    println(apiMethods.map(_.lift).map(methodDescription).mkString("\n"))

    // Generate JSON-RPC wrapper functions for the API methods
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = ref.callTerm(api.asTerm, methodName, List.empty, List.empty)

    // Generate function call using a type parameter
    val typeParam = TypeRepr.of[List[List[String]]]
    val typedCall = ref.callTerm('{ List }.asTerm, "apply", List(typeParam), List.empty)

    // Debug printounts
    println(
      s"""
        |Call:
        |  $call
        |
        |Typed call:
        |  $typedCall
        |""".stripMargin
    )

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
      if method.typeParams.nonEmpty then
        sys.error(s"Bound API method must not have type parameters: $signature")
      else if !method.available then
        sys.error(s"Bound API method must be callable at runtime: $signature")
    }
    methods

  private def generateMethodHandle[Node: Type, Outcome[_]: Type, CodecType <: Codec[Node]: Type, Context: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]]
  ): Expr[(String, MethodHandle[Node, Outcome, Context])] =
    given Quotes = ref.quotes
    val liftedMethod = method.lift
    val function = generateFunction[Node, Outcome, CodecType, Context](ref, method, codec, effect)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val paramNames = Expr(liftedMethod.params.flatMap(_.map(_.name)))
    val paramTypes = Expr(liftedMethod.params.flatMap(_.map(_.dataType)))
    '{
      $name -> MethodHandle($function, $name, $resultType, $paramNames, $paramTypes)
    }

  private def generateFunction[Node: Type, Outcome[_]: Type, CodecType <: Codec[Node]: Type, Context: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]]
  ): Expr[(Seq[Node], Option[Context]) => Outcome[Node]] =
    import ref.quotes.reflect.{asTerm, TypeRepr}
    given Quotes = ref.quotes

    method.params.map { param =>
//      val decodeCall = ref.callTerm(codec.asTerm, "decode", List(param.dataType), List(List(Expr("TEST").asTerm))).asExpr
    }
    val function = '{
      (arguments: Seq[Node], context: Option[Context]) =>
        $effect.pure(arguments.head)
    }
    println(function.asTerm)
    function

  private def methodDescription(method: Method): String =
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.signature}\n"
