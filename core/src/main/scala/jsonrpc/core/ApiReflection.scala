package jsonrpc.core

import jsonrpc.util.ValueOps.{asLeft, asRight}
import jsonrpc.util.{Empty, Reflection}
import scala.quoted.{Quotes, Type, quotes}

case object ApiReflection:
  def detectApiMethods[Outcome[_]: Type, Context: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree
  ): Seq[Either[(String, String), ref.QuotedMethod]] =
    import ref.quotes.reflect.{TypeRepr, TypeTree}
    given Quotes = ref.quotes

    val baseMethodNames = Seq(TypeRepr.of[AnyRef], TypeRepr.of[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val methods = ref.methods(apiType.tpe).filter(_.public).filter {
      method => !baseMethodNames.contains(method.symbol.name)
    }
    methods.map(method => validateApiMethod(ref, apiType, method))

  def contextSupplied[Context: Type](quotes: Quotes): Boolean =
    import quotes.reflect.TypeRepr
    given Quotes = quotes

    !(TypeRepr.of[Context] <:< TypeRepr.of[Empty[?]] ||
      TypeRepr.of[Context] =:= TypeRepr.of[None.type] ||
      TypeRepr.of[Context] =:= TypeRepr.of[Unit])

  private def validateApiMethod[Outcome[_]: Type, Context: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree,
    method: ref.QuotedMethod
  ): Either[(String, String), ref.QuotedMethod] =
    import ref.quotes.reflect.{AppliedType, LambdaType, NamedType, TypeRepr}

    val signature = s"${apiType.show}.${method.lift.signature}"
    val validatedMethod =
      if method.typeParameters.nonEmpty then
        s"Bound API method '$signature' must not have type parameters".asLeft
      else if !method.available then
        s"Bound API method '$signature' must be callable at runtime".asLeft
      else if contextSupplied[Context](ref.quotes) && method.parameters.lastOption.map { parameters =>
        !(parameters.last.dataType =:= TypeRepr.of[Context])
      }.getOrElse(true)
      then
        s"Bound API method '$signature' must accept the specified request context type '${TypeRepr.of[Context].show}' as its last parameter".asLeft
      else
        TypeRepr.of[Outcome] match
          case lambdaType: LambdaType =>
            if method.resultType match
              case appliedType: AppliedType => !(appliedType.tycon =:= lambdaType)
              case _                        => false
            then
              s"Bound API method '$signature' must return the specified effect type '${lambdaType.resType.show}'".asLeft
            else
              method.asRight
          case _ => method.asRight
    validatedMethod.swap.map(error => method.name -> error).swap
