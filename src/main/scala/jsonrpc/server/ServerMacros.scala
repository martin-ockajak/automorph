package jsonrpc.server

import jsonrpc.core.{Introspection, Method}
import scala.quoted.{Expr, Quotes, ToExpr, Type, quotes}

object ServerMacros:

  inline def bind[T <: AnyRef](inline api: T): Unit = ${bind('api)}

  private def bind[T <: AnyRef: Type](api: Expr[T])(using Quotes): Expr[Unit] =
    import quotes.reflect.*
    val apiTypeTree = TypeTree.of[T]
    val apiTypeSymbol = TypeRepr.of[T].typeSymbol
    val apiMethods = Introspection.publicApiMethods(apiTypeTree, concrete = true)
    val result = apiMethods.map(methodDescription).mkString("\n")
    val typeParam = TypeRepr.of[List[List[String]]]
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = Select.unique(api.asTerm, methodName).appliedToNone
    val typedCall = Select.unique('{List}.asTerm, "apply").appliedToType(typeParam).appliedTo('{List.empty}.asTerm)
    println(call.show)
    println(typedCall.show)
    '{
      println(${call.asExpr})
      println(${typedCall.asExpr})
      println()
      println(${Expr(result)})
    }

  private def methodDescription(method: Method): String =
    val paramLists = method.params.map { params =>
      s"(${params.map { param =>
        s"${param.name}: ${simpleTypeName(param.dataType)}"
      }.mkString(", ")})"
    }.mkString
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    val resultType = simpleTypeName(method.resultType)
    s"$documentation${method.name}$paramLists: $resultType\n"

  private def simpleTypeName(typeName: String): String =
    typeName.replaceAll("[^\\[\\], ]+\\.", "").nn
