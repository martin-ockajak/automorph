package jsonrpc.server

import jsonrpc.core.{Method, Reflection}
import jsonrpc.spi.{Codec, Effect}
import scala.quoted.{quotes, Expr, Quotes, Type}
import scala.compiletime.error
import scala.collection.immutable.ArraySeq

final case class MethodHandle[Node, Outcome[_]](
  function: Node => Outcome[Node],
  name: String,
  signature: String
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
   * @return mapping of method names to their JSON-RPC wrapper functions
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[ApiType <: AnyRef, Node, Outcome[_], CodecType <: Codec[Node]](
    codec: CodecType,
    effect: Effect[Outcome],
    api: ApiType
  ): Map[String, MethodHandle[Node, Outcome]] = ${ bind('codec, 'effect, 'api) }

  private def bind[ApiType <: AnyRef: Type, Node: Type, Outcome[_]: Type, CodecType <: Codec[Node]: Type](
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]],
    api: Expr[ApiType]
  )(using quotes: Quotes): Expr[Map[String, MethodHandle[Node, Outcome]]] =
    import ref.quotes.reflect.{TypeRepr, TypeTree}

    val ref = Reflection(quotes)

    def validateApiMethods(methods: Seq[ref.QuotedMethod]): Unit =
      methods.foreach { method =>
        val signature = s"${TypeTree.of[ApiType].show}.${method.lift.signature}"
        if method.typeParams.nonEmpty then
          throw IllegalArgumentException(s"Bound API method must not have type parameters: $signature")
        else if !method.available then
          throw IllegalArgumentException(s"Bound API method must be callable at runtime: $signature")
      }

    def methodArgument[T: Type](node: Node, param: ref.QuotedParam): Expr[T] =
      '{
        $codec.deserialize(ArraySeq.ofByte("".getBytes("UTF-8"))).asInstanceOf[T]
//        $codec.decode[T](node)
      }

//    def methodFunction(method: ref.QuoteDMethod): Expr[Node => Outcome[Node]] =
//      '{
//      }

    // Detect and validate public methods in the API type
    val baseMethodNames = Seq(TypeRepr.of[AnyRef], TypeRepr.of[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val apiMethods = ref.methods(TypeRepr.of[ApiType]).filter(_.public).filter {
      method => !baseMethodNames.contains(method.symbol.name)
    }
    validateApiMethods(apiMethods)

    // Generate JSON-RPC wrapper functions for the API methods
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = ref.callTerm(ref.term(api), methodName, List.empty, List.empty)

    // Generate function call using a type parameter
    val typeParam = TypeTree.of[List[List[String]]]
    val typedCall = ref.callTerm(ref.term('{ List }), "apply", List(typeParam), List.empty)

    val handles = Expr.ofSeq(Seq.empty[Expr[(String, MethodHandle[Node, Outcome])]])

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
    val apiDescription = apiMethods.map(_.lift).map(methodDescription).mkString("\n")
    println(apiDescription)

    // Generate printouts code using the previously generated code
    '{
//      println(${call.asExpr})
//      println(${typedCall.asExpr})
      println()
      $handles.toMap
    }

  private def methodDescription(method: Method): String =
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.signature}\n"
