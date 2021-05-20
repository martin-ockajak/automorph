package jsonrpc.server

import jsonrpc.core.{Method, Reflection}
import jsonrpc.spi.{Codec, Effect}
import scala.quoted.{Expr, Quotes, Type, quotes}


final case class FunctionHandle[Node, Outcome[_]](
  function: Node => Outcome[Node],
  name: String,
  prototype: String
)

object HandlerMacros:

  inline def bind[T <: AnyRef, Node, Outcome[_]](
    codec: Codec[Node],
    effect: Effect[Outcome],
    api: T
  ): Map[String, FunctionHandle[Node, Outcome]] = ${ bind('codec, 'effect, 'api) }

  private def bind[T <: AnyRef: Type, Node: Type, Outcome[_]: Type](
    codec: Expr[Codec[Node]],
    effect: Expr[Effect[Outcome]],
    api: Expr[T]
  )(using quotes: Quotes): Expr[Map[String, FunctionHandle[Node, Outcome]]] =
    import ref.quotes.reflect.TypeTree

    val ref = Reflection(quotes)

    // Detect the API type public methods
    val baseMethodNames = Seq(TypeTree.of[AnyRef], TypeTree.of[Product]).flatMap {
      typeTree => ref.methods(typeTree).filter(_.public).map(_.name)
    }.toSet
    val apiMethods = ref.methods(TypeTree.of[T]).filter(_.public).filter {
      method => !baseMethodNames.contains(method.symbol.name)
    }

    // Disallow API types unavailable methods which cannot be invoke at runtime
    apiMethods.filterNot(_.available).foreach {
      method => throw new IllegalStateException(s"Invalid API method: ${method.symbol.fullName}")
    }

    // Generate method call code
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = ref.callTerm(ref.term(api), methodName, List.empty, List.empty)

    // Generate function call using a type parameter
    val typeParam = TypeTree.of[List[List[String]]]
    val typedCall = ref.callTerm(ref.term('{ List }), "apply", List(typeParam), List.empty)

    val handles = Expr.ofSeq(Seq.empty[Expr[(String, FunctionHandle[Node, Outcome])]])

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
    val paramLists = method.params.map { params =>
      s"(${params.map { param =>
        s"${param.name}: ${param.dataType}"
      }.mkString(", ")})"
    }.mkString
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.name}$paramLists: ${method.resultType}\n"
