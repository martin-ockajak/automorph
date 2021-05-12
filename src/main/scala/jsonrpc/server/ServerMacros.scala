package jsonrpc.server

import scala.quoted.{Expr, Quotes, Type, quotes}

object ServerMacros:
  inline def bind[T](inline api: T): Unit =
    ${ bindMeta('api) }

  private def bindMeta[T: Type](api: Expr[T])(using Quotes): Expr[Unit] = {
    import quotes.reflect.*
    val apiType: TypeRepr = TypeRepr.of[T]
    '{println(${api}.toString)}
//    '{println(apiType.asType)}
  }

  inline def print(inline text: String): Unit =
    ${ printImpl('text) }

  private def printImpl(text: Expr[String])(using Quotes): Expr[Unit] =
    '{println(${text})}
