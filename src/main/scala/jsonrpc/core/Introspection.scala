package jsonrpc.core

import scala.quoted.{Quotes, Type, quotes}

final class Introspection(using Quotes):
  import quotes.reflect.{Flags, MethodType, TypeRepr, TypeTree}

  private val abstractApiMethodFlags = Seq(
    Flags.Deferred,
    Flags.Erased,
    Flags.Inline,
    Flags.Invisible,
    Flags.Macro,
    Flags.Transparent,
  )
  private val omittedApiMethodFlags = Seq(
    Flags.Private,
    Flags.PrivateLocal,
    Flags.Protected,
    Flags.Synthetic,
  )
  private val baseMethodNames =
    val anyRefMethods = publicMethods(TypeTree.of[AnyRef])
    val productMethods = publicMethods(TypeTree.of[Product])
    (anyRefMethods ++ productMethods).map(_.name).toSet

  def publicApiMethods[T <: AnyRef: Type](concrete: Boolean): Seq[Introspection.Method] =
    val classTypeTree = TypeTree.of[T]
    val validMethods = publicMethods(classTypeTree).filter(validApiMethod(_, concrete))
    validMethods.flatMap(methodSymbol => methodDescriptor(classTypeTree, methodSymbol))

  private def methodDescriptor(classTypeTree: quotes.reflect.TypeTree, methodSymbol: quotes.reflect.Symbol): Option[Introspection.Method] =
    classTypeTree.tpe.memberType(methodSymbol) match
      case methodType: MethodType =>
        val methodTypes = LazyList.iterate(Option(methodType)) {
          case Some(currentType) => MethodType.unapply(currentType) match
            case (_, _, resultType: MethodType) => Some(resultType)
            case _ => None
          case _ => None
        }.takeWhile(_.isDefined).flatten
        val (_, _, resultType) = MethodType.unapply(methodTypes.last)
        val params = methodTypes.map { currentType =>
          val (paramNames, paramTypes, resultType) = MethodType.unapply(currentType)
          paramNames.zip(paramTypes).map((paramName, paramType) => Introspection.Param(paramName, paramType.show))
        }
        Some(Introspection.Method(
          methodSymbol.name,
          resultType.show,
          params,
          methodSymbol.docstring
        ))
      case _ => None

  private def publicMethods(classTypeTree: quotes.reflect.TypeTree): Seq[quotes.reflect.Symbol] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberMethods.filter(methodSymbol => !matchesFlags(methodSymbol.flags, omittedApiMethodFlags))

  private def validApiMethod(methodSymbol: quotes.reflect.Symbol, concrete: Boolean): Boolean =
    methodSymbol.flags.is(Flags.Method) &&
      !baseMethodNames.contains(methodSymbol.name) && (
      if concrete && matchesFlags(methodSymbol.flags, abstractApiMethodFlags) then
        throw new IllegalStateException(s"Invalid API method: ${methodSymbol.fullName}")
      else
        true
      )

  private def matchesFlags(flags: quotes.reflect.Flags, matchingFlags: Seq[quotes.reflect.Flags]): Boolean =
    matchingFlags.foldLeft(false) { (result, currentFlags) =>
      result | flags.is(currentFlags)
    }

object Introspection:
  final case class Param(
    name: String,
    dataType: String,
  )

  final case class Method(
    name: String,
    resultType: String,
    params: Seq[Seq[Param]],
    documentation: Option[String],
  )
