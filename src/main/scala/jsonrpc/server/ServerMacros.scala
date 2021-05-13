package jsonrpc.server

import java.lang.reflect.{Field, Method}
import scala.quoted.{Expr, Quotes, Type, quotes}
import deriving.Mirror
import scala.reflect.ClassTag
import scala.language.unsafeNulls

object ServerMacros:

  inline def bind[T <: AnyRef](inline api: T): Unit = ${bind('api)}

  private def bind[T <: AnyRef: Type](api: Expr[T])(using q: Quotes): Expr[Unit] =
    import quotes.reflect.*
    val apiTypeSymbol = TypeRepr.of[T].typeSymbol
    val apiMethods = publicApiMethods(apiTypeSymbol)
    val result = apiMethods.map(methodDescription).mkString("\n")
    '{
      println(${Expr(result)})
    }

  private def publicApiMethods(using Quotes)(classSymbol: quotes.reflect.Symbol): Seq[Method] =
    val validMethods = publicMethods(classSymbol).filter(validApiMethod)
    validMethods.foreach { methodSymbol =>
      println(methodSymbol.signature)
    }
    validMethods.map { methodSymbol =>
      val argumentTypes = methodArgumentTypes(methodSymbol)
      Method(
        methodSymbol.name,
        methodSymbol.signature.resultSig,
        methodSymbol.paramSymss.zip(argumentTypes).map((symbols, types) => (symbols.zip(types)).map { (symbol, dataType) =>
          Argument(symbol.name, dataType)
        }),
        methodSymbol.docstring
      )
    }

  private def methodArgumentTypes(using Quotes)(methodSymbol: quotes.reflect.Symbol): Seq[Seq[String]] =
    val argumentListSizes = methodSymbol.paramSymss.map(_.size)
    val flatArgumentTypes = methodSymbol.signature.paramSigs.flatMap {
      case dataType: String => Seq(dataType)
      case _: Int => Seq.empty[String]
    }
    argumentListSizes.iterator.foldLeft((List.empty[List[String]], flatArgumentTypes)) { (result, size) =>
      val (init, tail) = result(1).splitAt(size)
      (result(0) :+ init, tail)
    }.head

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

  private def baseMethodNames(using Quotes): Set[String] =
    import quotes.reflect.*
    val anyRefMethods = publicMethods(TypeRepr.of[AnyRef].typeSymbol)
    val productMethods = publicMethods(TypeRepr.of[Product].typeSymbol)
    (anyRefMethods ++ productMethods).map(_.name).toSet

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

  private final case class Argument(
    name: String,
    dataType: String,
  )

  private final case class Method(
    name: String,
    resultType: String,
    arguments: Seq[Seq[Argument]],
    documentation: Option[String],
  )
