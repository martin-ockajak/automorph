package jsonrpc.core

import scala.quoted.{Quotes, Type, quotes}

object Introspection:

  def publicApiMethods(using Quotes)(classTypeTree: quotes.reflect.TypeTree, concrete: Boolean): Seq[Method] =
    import quotes.reflect.MethodType
    val validMethods = publicMethods(classTypeTree).filter(validApiMethod(_, concrete))
    validMethods.flatMap(methodSymbol => methodDescriptor(classTypeTree, methodSymbol))

  private def methodDescriptor(using Quotes)(classTypeTree: quotes.reflect.TypeTree, methodSymbol: quotes.reflect.Symbol): Option[Method] =
    import quotes.reflect.{MethodType, TypeRepr}
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
          paramNames.zip(paramTypes).map((paramName, paramType) => Param(paramName, paramType.show))
        }
        Some(Method(
          methodSymbol.name,
          resultType.show,
          params,
          methodSymbol.docstring
        ))
      case _ => None

  private def publicMethods(using Quotes)(classTypeTree: quotes.reflect.TypeTree): Seq[quotes.reflect.Symbol] = {
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberMethods.filter(methodSymbol => !matchesFlags(methodSymbol.flags, omittedApiMethodFlags))
  }

  private def validApiMethod(using Quotes)(methodSymbol: quotes.reflect.Symbol, concrete: Boolean): Boolean = {
    import quotes.reflect.Flags
    methodSymbol.flags.is(Flags.Method) &&
      !baseMethodNames.contains(methodSymbol.name) && (
      if concrete && matchesFlags(methodSymbol.flags, abstractApiMethodFlags) then
        throw new IllegalStateException(s"Invalid API method: ${methodSymbol.fullName}")
      else
        true
      )
  }

  private def abstractApiMethodFlags(using Quotes): Seq[quotes.reflect.Flags] =
    import quotes.reflect.Flags
    Seq(
      Flags.Deferred,
      Flags.Erased,
      Flags.Inline,
      Flags.Invisible,
      Flags.Macro,
      Flags.Transparent,
    )

  private def omittedApiMethodFlags(using Quotes): Seq[quotes.reflect.Flags] =
    import quotes.reflect.Flags
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
    import quotes.reflect.TypeTree
    val anyRefMethods = publicMethods(TypeTree.of[AnyRef])
    val productMethods = publicMethods(TypeTree.of[Product])
    (anyRefMethods ++ productMethods).map(_.name).toSet

  private def methodDescription(method: Method): String =
    val paramLists = method.params.map { params =>
      s"(${params.map { param =>
        s"${param.name}: ${simpleTypeName(param.dataType)}"
      }.mkString(", ")})"
    }.mkString
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    val resultType = simpleTypeName(method.resultType)
    s"$documentation${method.name}$paramLists: $resultType\n"

  private def simpleTypeName(typeName: String): String = {
    typeName.split("\\.").asInstanceOf[Array[String]].lastOption.getOrElse("")
  }

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
