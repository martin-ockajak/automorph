package jsonrpc.server

import jsonrpc.core.Reflection
import jsonrpc.spi.Codec
import jsonrpc.spi.Effect
import scala.quoted.{quotes, Expr, Quotes, Type}


final case class FunctionHandle[Node, Outcome[_]](
  function: Node => Outcome[Node],
  prototype: String
)

object HandlerMacros:

  inline def bind[T <: AnyRef, Node, Outcome[_]](
    codec: Codec[Node],
    effect: Effect[Outcome],
    api: T
  ): Map[String, FunctionHandle[Node, Outcome]] = ${ bind('codec, 'effect, 'api) }

  private def bind[T <: AnyRef: Type, Node, Outcome[_]](
    codec: Expr[Codec[Node]],
    effect: Expr[Effect[Outcome]],
    api: Expr[T]
  )(using quotes: Quotes): Expr[Map[String, FunctionHandle[Node, Outcome]]] =
    import ref.quotes.reflect.*

    val ref = Reflection(quotes)
    val baseMethodNames = Seq(TypeTree.of[AnyRef], TypeTree.of[Product]).flatMap {
      typeTree => ref.methods(typeTree).filter(_.public).map(_.name)
    }.toSet

//    def methodDescription(method: ref.Method): String =
//      val paramLists = method.params.map { params =>
//        s"(${params.map { param =>
//          s"${param.name}: ${simpleTypeName(param.dataType.show)}"
//        }.mkString(", ")})"
//      }.mkString
//
//      val documentation = method.symbol.docstring.map(_ + "\n").getOrElse("")
//      val resultType = simpleTypeName(method.resultType.show)
//      s"$documentation${method.name}$paramLists: $resultType\n"

    // Introspect the API instance & generate its description
    val apiMethods = ref.methods(TypeTree.of[T]).filter(_.public).filter {
      method => !baseMethodNames.contains(method.symbol.name)
    }
    apiMethods.filterNot(_.available).foreach {
      method => throw new IllegalStateException(s"Invalid API method: ${method.symbol.fullName}")
    }
//    val apiDescription = apiMethods.map(methodDescription).mkString("\n")

    // Generate method call code
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
//      println(${ Expr(apiDescription) })
      Map.empty
    }

  private def simpleTypeName(typeName: String): String =
    typeName.replaceAll("[^\\[\\], ]+\\.", "")
