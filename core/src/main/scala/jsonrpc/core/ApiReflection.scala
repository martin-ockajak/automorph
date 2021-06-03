package jsonrpc.core

import jsonrpc.util.ValueOps.{asLeft, asRight}
import jsonrpc.util.{Empty, Reflection}
import scala.quoted.{quotes, Quotes, Type}

case object ApiReflection:

  /**
   * Detect valid API methods in the specified API type.
   *
   * @param ref reflection
   * @param apiType API type
   * @tparam Outcome effectful computation outcome type
   * @tparam Context request context type
   * @return valid method descriptors or error messages by method name
   */
  def detectApiMethods[Outcome[_]: Type, Context: Type](
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
   * Determine whether a request context type is empty.
   *
   * @param quotes quotation context
   * @tparam Context request context type
   * @return true if the context type is empty, false otherwise
   */
  def contextEmpty[Context: Type](quotes: Quotes): Boolean =
    import quotes.reflect.{AppliedType, TypeRepr}
    given Quotes = quotes

    val contextType = TypeRepr.of[Context]
    contextType match
      case appliedType: AppliedType => appliedType.tycon =:= TypeRepr.of[Empty]
      case _                        => contextType =:= TypeRepr.of[None.type] || contextType =:= TypeRepr.of[Unit]

  /**
   * Determine whether a method is a valid API method.
   *
   * @param ref reflection context
   * @param apiType API type
   * @param method method
   * @tparam Outcome effectful computation outcome type
   * @tparam Context request context type
   * @return valid API method or an error message
   */
  private def validateApiMethod[Outcome[_]: Type, Context: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree,
    method: ref.QuotedMethod
  ): Either[String, ref.QuotedMethod] =
    import ref.quotes.reflect.{AppliedType, LambdaType, NamedType, TypeRepr}

    val signature = s"${apiType.show}.${method.lift.signature}"
    if method.typeParameters.nonEmpty then
      s"Bound API method '$signature' must not have type parameters".asLeft
    else if !method.available then
      s"Bound API method '$signature' must be callable at runtime".asLeft
    else if !contextEmpty[Context](ref.quotes) && method.parameters.lastOption.map { parameters =>
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
