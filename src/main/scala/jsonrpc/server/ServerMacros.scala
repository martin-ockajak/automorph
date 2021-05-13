package jsonrpc.server

import jsonrpc.core.{Introspection, Method}
import scala.quoted.{Expr, Quotes, ToExpr, Type, quotes}

object ServerMacros:

  inline def bind[T <: AnyRef](inline api: T): Unit = ${bind('api)}

  private def bind[T <: AnyRef: Type](api: Expr[T])(using Quotes): Expr[Unit] =
    import quotes.reflect.*
    val apiTypeSymbol = TypeRepr.of[T].typeSymbol
    val apiMethods = Introspection.publicApiMethods(apiTypeSymbol, concrete = true)
    val result = apiMethods.map(methodDescription).mkString("\n")
    val typeParam = TypeRepr.of[List[List[String]]]
    val methodName = apiMethods.find(_.arguments.flatten.size == 0).map(_.name).getOrElse("")
    val call = Select.unique(api.asTerm, methodName).appliedToNone
    val typedCall = Select.unique('{List}.asTerm, "apply").appliedToType(typeParam).appliedTo('{List.empty[List[String]]}.asTerm)
    println(call.show)
    println(typedCall.show)
    '{
      println(${call.asExpr})
      println(${typedCall.asExpr})
      println()
      println(${Expr(result)})
    }

  private def methodDescription(method: Method): String =
    val argumentLists = method.arguments.map { arguments =>
      s"(${arguments.map { argument =>
        s"${argument.name}: ${simpleTypeName(argument.dataType)}"
      }.mkString(", ")})"
    }.mkString
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    val resultType = simpleTypeName(method.resultType)
    s"$documentation${method.name}$argumentLists: $resultType\n"

  private def simpleTypeName(typeName: String): String = {
    typeName.split("\\.").asInstanceOf[Array[String]].lastOption.getOrElse("")
  }
