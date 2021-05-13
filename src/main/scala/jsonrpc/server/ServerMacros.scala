package jsonrpc.server

import java.lang.reflect.{Field, Method}
import scala.quoted.{Expr, Quotes, Type, quotes}
import deriving.Mirror
import scala.reflect.ClassTag

object ServerMacros:

  inline def bind[T <: AnyRef](inline api: T): Unit =
    ${ bind('api) }

  private def bind[T <: AnyRef: Type](api: Expr[T])(using q: Quotes): Expr[Unit] =
    import quotes.reflect.*
    val apiTypeSymbol = TypeRepr.of[T].typeSymbol
    val apiMethods = publicApiMethods(apiTypeSymbol)
    val result = apiMethods.map { method =>
      val arguments = method.arguments.flatten.map(_.name).mkString(", ")
      val documentation = method.documentation.map(_ + "\n").getOrElse("")
      s"$documentation${method.name}($arguments)\n"
    }.mkString("\n")
    println(result)
    '{
      println($api) // the name of the Api, which now is a case class (with toString)
    }

  private def publicApiMethods(using Quotes)(classSymbol: quotes.reflect.Symbol): Seq[Method] = {
    val validMethods = publicMethods(classSymbol).filter(validApiMethod)
    validMethods.map(methodSymbol => Method(
      methodSymbol.name,
      methodSymbol.name,
      methodSymbol.paramSymss.map(_.map { paramSymbol =>
        Value(paramSymbol.name, paramSymbol.name)
      }),
      methodSymbol.docstring
    ))
  }

  private def publicMethods(using Quotes)(classSymbol: quotes.reflect.Symbol): Seq[quotes.reflect.Symbol] =
    classSymbol.memberMethods.filter(methodSymbol => !matchesFlags(methodSymbol.flags, omittedApiMethodFlags))

  private def validApiMethod(using Quotes)(methodSymbol: quotes.reflect.Symbol): Boolean =
    import quotes.reflect.*
    methodSymbol.flags.is(Flags.Method) &&
      !baseMethodNames.contains(methodSymbol.name) && (
      if matchesFlags(methodSymbol.flags, invalidApiMethodFlags) then
        throw new IllegalStateException(s"Invalid API method: ${methodSymbol.fullName}")
      else
        true
      )

  private def invalidApiMethodFlags(using Quotes): Seq[quotes.reflect.Flags] =
    import quotes.reflect.*
    Seq(
      Flags.Deferred,
      Flags.Erased,
      Flags.Inline,
      Flags.Invisible,
      Flags.Macro,
      Flags.Transparent,
    )

  private def omittedApiMethodFlags(using Quotes): Seq[quotes.reflect.Flags] =
    import quotes.reflect.*
    Seq(
      Flags.Private,
      Flags.PrivateLocal,
      Flags.Protected,
      Flags.Synthetic,
    )

  private def matchesFlags(using Quotes)(flags: quotes.reflect.Flags, matchingFlags: Seq[quotes.reflect.Flags]): Boolean =
    matchingFlags.foldLeft(false) { (result, currentFlags) =>
      result | flags.is(currentFlags)
    }

  private def baseMethodNames(using Quotes): Set[String] = {
    import quotes.reflect.*
    val anyRefMethods = publicMethods(TypeRepr.of[AnyRef].typeSymbol)
    val productMethods = publicMethods(TypeRepr.of[Product].typeSymbol)
    (anyRefMethods ++ productMethods).map(_.name).toSet
  }

  private final case class Value(
    name: String,
    dataType: String,
  )

  private final case class Method(
    name: String,
    returnType: String,
    arguments: Seq[Seq[Value]],
    documentation: Option[String],
  )
