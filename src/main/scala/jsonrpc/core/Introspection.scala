package jsonrpc.core

import scala.quoted.{Quotes, Type, quotes}

final class Introspection(val quotes: Quotes):
  val reflect = quotes.reflect
  given Quotes = quotes
  import reflect.{Flags, MethodType, Symbol, TypeRepr, TypeTree}

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

  def publicApiMethods[T <: AnyRef: Type](concrete: Boolean): Seq[Introspection.Method[TypeRepr]] =
    val classTypeTree = TypeTree.of[T]
    val validMethods = publicMethods(classTypeTree).filter(validApiMethod(_, concrete))
    validMethods.flatMap(methodSymbol => methodDescriptor(classTypeTree, methodSymbol))

  private def methodDescriptor(classTypeTree: TypeTree, methodSymbol: Symbol): Option[Introspection.Method[TypeRepr]] =
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
          paramNames.zip(paramTypes).map((paramName, paramType) => Introspection.Param(paramName, paramType))
        }
        Some(Introspection.Method[TypeRepr](
          methodSymbol.name,
          resultType,
          params,
          methodSymbol.docstring
        ))
      case _ => None

  private def publicMethods(classTypeTree: TypeTree): Seq[Symbol] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberMethods.filter(methodSymbol => !matchesFlags(methodSymbol.flags, omittedApiMethodFlags))

  private def validApiMethod(methodSymbol: Symbol, concrete: Boolean): Boolean =
    methodSymbol.flags.is(Flags.Method) &&
      !baseMethodNames.contains(methodSymbol.name) && (
      if concrete && matchesFlags(methodSymbol.flags, abstractApiMethodFlags) then
        throw new IllegalStateException(s"Invalid API method: ${methodSymbol.fullName}")
      else
        true
      )

  private def matchesFlags(flags: Flags, matchingFlags: Seq[Flags]): Boolean =
    matchingFlags.foldLeft(false)((result, current) => result | flags.is(current))

object Introspection:
  final case class Param[T](
    name: String,
    dataType: T,
  )

  final case class Method[T](
    name: String,
    resultType: T,
    params: Seq[Seq[Param[T]]],
    documentation: Option[String],
  )
