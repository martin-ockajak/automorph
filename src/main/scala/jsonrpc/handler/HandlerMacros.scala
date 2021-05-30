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

    // Generate API method handles including wrapper functions consuming and product Node values
    val methodHandles = Expr.ofSeq(apiMethods.map { method =>
      generateMethodHandle[Node, CodecType, Outcome, Context](ref, method, codec, effect)
    })

    // Generate function call using a type parameter
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = ref.callTerm(api.asTerm, methodName, List.empty, List.empty)
    val typeParam = TypeRepr.of[List[List[String]]]
    val typedCall = ref.callTerm('{ List }.asTerm, "apply", List(typeParam), List.empty)

    // Debug printounts
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
      if method.typeParams.nonEmpty then
        sys.error(s"Bound API method must not have type parameters: $signature")
      else if !method.available then
        sys.error(s"Bound API method must be callable at runtime: $signature")
    }
    methods

  private def generateMethodHandle[Node: Type, CodecType <: Codec[Node]: Type, Outcome[_]: Type, Context: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]]
  ): Expr[(String, MethodHandle[Node, Outcome, Context])] =
    given Quotes = ref.quotes
    val liftedMethod = method.lift
    val function = generateFunction[Node, CodecType, Outcome, Context](ref, method, codec, effect)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val paramNames = Expr(liftedMethod.params.flatMap(_.map(_.name)))
    val paramTypes = Expr(liftedMethod.params.flatMap(_.map(_.dataType)))
    '{
      $name -> MethodHandle($function, $name, $resultType, $paramNames, $paramTypes)
    }

  private def generateFunction[Node: Type, CodecType <: Codec[Node]: Type, Outcome[_]: Type, Context: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]]
  ): Expr[(Seq[Node], Option[Context]) => Outcome[Node]] =
    import ref.quotes.reflect.{asTerm, Lambda, MethodType, Printer, Symbol, Term, TypeRepr}
    given Quotes = ref.quotes

    println(method.name)
    method.params.flatMap(_.map { param =>
      param.dataType.asType match
        case '[paramType] =>
          val methodType = MethodType(List("argument"))(_ => List(TypeRepr.of[Node]), _ => param.dataType)
          val lambda = Lambda(Symbol.spliceOwner, methodType, (symbol, args) =>
            ref.callTerm(codec.asTerm, "decode", List(param.dataType), List(args.asInstanceOf[List[Term]]))
          )
          println(s"  ${lambda.show(using Printer.TreeCode)}")
//          println(s"  $lambda")
          lambda
    })
    println()
    val function = '{
    (arguments: Seq[Node], context: Option[Context]) =>
        $effect.pure(arguments.head)
    }
//    println(function.asTerm)
//  Inlined(List(DefDef($anonfun,List(List(ValDef(argument,TypeTree - argument,EmptyTree))),TypeTree - Node,Block(List(), callTerm(codec.asTerm, "decode", List(param.dataType), List(Select(Ident(argument))))))
//  Block(List(DefDef("$anonfun", List(TermParamClause(List(ValDef("argument", Inferred(), None)))), Inferred(), Some(Block(Nil, Apply(Select(Inlined(None, Nil, Inlined(None, Nil, Select(Ident("JsonRpcHandler_this"), "codec"))), "deserialize"), List(TypeApply(Select(Ident("argument"), "asInstanceOf"), List(TypeSelect(Ident("ArraySeq"), "ofByte"))))))))), Closure(Ident("$anonfun"), None))
//  Lambda(Symbol.spliceOwner, MethodType()(_ => List[TypeRepr], _ => TypeRepr), (symbol, args) => Tree)
    function

  private def methodDescription(method: Method): String =
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.signature}\n"
