package jsonrpc.server

import jsonrpc.core.Introspection
import scala.quoted.{Expr, Quotes, Type, quotes}

object ServerMacros:

  inline def bind[T <: AnyRef](inline api: T): Unit = ${bind('api)}

  private def bind[T <: AnyRef: Type](api: Expr[T])(using Quotes): Expr[Unit] =
    import quotes.reflect.*
    val introspection = Introspection()
    val apiTypeTree = TypeTree.of[T]
    val apiMethods = introspection.publicApiMethods[T](concrete = true)
    val apiDescription = apiMethods.map(methodDescription).mkString("\n")
    val typeParam = TypeRepr.of[List[List[String]]]
    val methodName = apiMethods.find(_.params.flatten.isEmpty).map(_.name).getOrElse("")
    val call = Select.unique(api.asTerm, methodName).appliedToNone
    val typedCall = Select.unique('{List}.asTerm, "apply").appliedToType(typeParam).appliedTo('{List.empty}.asTerm)
    val classDef = '{
      class Test(
        a: String,
        b: Int
      )
    }
    println(
      s"""
        |Call:
        |  ${call}
        |
        |Typed call:
        |  ${typedCall}
        |
        |Class definition:
        |  ${classDef.asTerm}
        |""".stripMargin)
    '{
      println(${call.asExpr})
      println(${typedCall.asExpr})
      println()
      println(${Expr(apiDescription)})
    }

  private def methodDescription(method: Introspection.Method): String =
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
