package jsonrpc.server

import jsonrpc.spi.{Codec, Effect}
import jsonrpc.util.{Method, Reflection}
import scala.collection.immutable.ArraySeq
import scala.compiletime.error
import scala.quoted.{quotes, Expr, Quotes, Type}

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
    import ref.quotes.reflect.{TypeRepr, TypeTree}

    val ref = Reflection(quotes)

    def methodArgument[T: Type](node: Expr[Node], param: ref.QuotedParam): Expr[T] =
      '{
        $codec.decode[T]($node)
      }

//    def methodFunction(method: ref.QuoteDMethod): Expr[Node => Outcome[Node]] =
//      '{
//      }

    // Detect and validate public methods in the API type
    val apiMethods = detectApiMethods(ref, TypeTree.of[ApiType])
    val methodHandles = Expr.ofSeq(apiMethods.map(method => methodHandle[Node, Outcome, Context](ref, method)))
    println(apiMethods.map(_.lift).map(methodDescription).mkString("\n"))

    // Generate JSON-RPC wrapper functions for the API methods
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = ref.callTerm(ref.term(api), methodName, List.empty, List.empty)

    // Generate function call using a type parameter
    val typeParam = TypeTree.of[List[List[String]]]
    val typedCall = ref.callTerm(ref.term('{ List }), "apply", List(typeParam), List.empty)

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
      println()
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

  private def methodHandle[Node, Outcome[_], Context](
    ref: Reflection,
    method: ref.QuotedMethod
  ): Expr[(String, MethodHandle[Node, Outcome, Context])] = ???

  private def methodDescription(method: Method): String =
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.signature}\n"
