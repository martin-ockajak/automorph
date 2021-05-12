package jsonrpc.server

import java.lang.reflect.{Field, Method}
import scala.quoted.{Expr, Quotes, Type, quotes}
import deriving.Mirror
import scala.reflect.ClassTag

object ServerMacros:
  inline def bind[T <: AnyRef](inline api: T): Unit =
    ${ bindMeta('api) }

  private def bindMeta[T: Type](api: Expr[T])(using q: Quotes): Expr[Unit] =
    import quotes.reflect.*
    // summon[Type[T]]
    // Type.of[T]
    // TypeTree.of[T].symbol
    val apiType = TypeRepr.of[T]
    apiType.classSymbol.foreach { symbol =>
      println(symbol.name)
      println(symbol.declaredMethods)
    }
    '{
      println($api) // the name of the Api, which now is a case class (with toString)
    }

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
