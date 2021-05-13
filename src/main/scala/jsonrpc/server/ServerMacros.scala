package jsonrpc.server

import scala.quoted.{Expr, Quotes, Type, quotes}

object ServerMacros:

  inline def bind[T <: AnyRef](inline api: T): Unit = ${bind('api)}

  private def bind[T <: AnyRef: Type](api: Expr[T])(using q: Quotes): Expr[Unit] =
    import quotes.reflect.*
    val apiTypeSymbol = TypeRepr.of[T].typeSymbol
    val apiMethods = Introspection.publicApiMethods(apiTypeSymbol, concrete = true)
    val result = apiMethods.map(methodDescription).mkString("\n")
    '{
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
