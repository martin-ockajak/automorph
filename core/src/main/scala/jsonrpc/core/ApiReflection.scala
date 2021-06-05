package jsonrpc.core

import jsonrpc.util.ValueOps.{asLeft, asRight}
import jsonrpc.util.Reflection
import scala.quoted.{quotes, Quotes, Type}

case object ApiReflection:

  /**
   * Detect valid API methods in the specified API type.
   *
   * @param ref reflection
   * @param apiType API type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return valid method descriptors or error messages by method name
   */
  def detectApiMethods[Effect[_]: Type, Context: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree
  ): Seq[Either[String, ref.QuotedMethod]] =
    import ref.quotes.reflect.{TypeRepr, TypeTree}
    given Quotes = ref.quotes

    val baseMethodNames = Seq(TypeRepr.of[AnyRef], TypeRepr.of[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val methods = ref.methods(apiType.tpe).filter(_.public).filter {
      method => !baseMethodNames.contains(method.symbol.name)
    }
    methods.map(method => validateApiMethod(ref, apiType, method))

  /**
   * Determine whether a method is a valid API method.
   *
   * @param ref reflection context
   * @param apiType API type
   * @param method method
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return valid API method or an error message
   */
  private def validateApiMethod[Effect[_]: Type, Context: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree,
    method: ref.QuotedMethod
  ): Either[String, ref.QuotedMethod] =
    import ref.quotes.reflect.{AppliedType, LambdaType, NamedType, TypeRepr}

    // No type parameters
    val signature = s"${apiType.show}.${method.lift.signature}"
    if method.typeParameters.nonEmpty then
      s"Bound API method '$signature' must not have type parameters".asLeft

    // Callable at runtime
    else if !method.available then
      s"Bound API method '$signature' must be callable at runtime".asLeft
    else
      // Returns the effect type
      val effectType =
        TypeRepr.of[Effect] match
          case lambdaType: LambdaType => lambdaType.resType
          case otherType              => otherType
      val resultEffectType =
        effectType match
          case appliedEffectType: AppliedType =>
            method.resultType match
              case resultType: AppliedType => resultType.tycon =:= appliedEffectType.tycon
              case _                       => false
          case _ => true
      if !resultEffectType then
        s"Bound API method '$signature' must return the specified effect type '${effectType.show}'".asLeft
      else
        method.asRight
