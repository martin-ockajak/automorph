package jsonrpc.server

import jsonrpc.core.Reflection
import scala.quoted.{Expr, Quotes, Type, quotes}

object ServerMacros:

  inline def bind[T <: AnyRef](inline api: T): Unit = ${bind('api)}

  private def bind[T <: AnyRef: Type](api: Expr[T])(using quotes: Quotes): Expr[Unit] =
    import quotes.reflect.*

    val ref = Reflection(quotes)

    def methodDescription(method: ref.Method): String =
      val paramLists = method.params.map { params =>
        s"(${params.map { param =>
          s"${param.name}: ${simpleTypeName(param.dataType.show)}"
        }.mkString(", ")})"
      }.mkString
      val documentation = method.symbol.docstring.map(_ + "\n").getOrElse("")
      val resultType = simpleTypeName(method.resultType.show)
      s"$documentation${method.name}$paramLists: $resultType\n"

    // Introspect the API instance & generate its description
    val apiTypeTree = ref.ast.TypeTree.of[T]
    val apiMethods = ref.publicApiMethods(apiTypeTree, concrete = true)
    val apiDescription = apiMethods.map(methodDescription).mkString("\n")

    // Generate method call code
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = ref.call(ref.term(api), methodName, List.empty, List.empty)

    // Generate function call using a type parameter
    val typeParam = ref.ast.TypeTree.of[List[List[String]]]
    val typedCall = ref.call(ref.term('{List}), "apply", List(typeParam), List.empty)

    // Debug printounts
    println(
      s"""
        |Call:
        |  ${call}
        |
        |Typed call:
        |  ${typedCall}
        |""".stripMargin)

    // Generate printouts code using the previously generated code
    '{
//      println(${call.asExpr})
//      println(${typedCall.asExpr})
      println()
      println(${Expr(apiDescription)})
    }

  private def simpleTypeName(typeName: String): String =
    typeName.replaceAll("[^\\[\\], ]+\\.", "").nn
