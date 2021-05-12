package jsonrpc.server

import java.lang.reflect.{Field, Method}
import scala.quoted.{Expr, Quotes, Type, quotes}

object ServerMacros:
  inline def bind[T](inline api: T): Unit =
    ${ bindMeta('api) }

  private def bindMeta[T: Type](api: Expr[T])(using Quotes): Expr[Unit] =
    import quotes.reflect.*
    val apiType = TypeRepr.of[T].asType
    val apiTypeSymbol = TypeTree.of[T].symbol
    inspect(apiTypeSymbol)
    println(apiType)
    println(apiTypeSymbol.tree)
    '{
      println($api) // the name of the Api, which now is a case class (with toString)
    }

  inline def print(inline text: String): Unit =
    ${ printImpl('text) }

  private def printImpl(text: Expr[String])(using Quotes): Expr[Unit] =
    '{println(${text})}

  private def inspect(value: Any): Unit =
    println(s"Name: \n  ${value.getClass.getName}")
    val fields = value.getClass.getFields match
      case value: Array[?] => value
      case _: Null => Array.empty[Field]
    println("Fields:")
    for
      field <- fields
    do
      println(s"  ${field.getName}")
    val methods = value.getClass.getMethods match
      case value: Array[?] => value
      case _: Null => Array.empty[Method]
    println("Methods:")
    for
      method <- methods
    do
      println(s"  ${method.getName}")
