package jsonrpc.core

import jsonrpc.util.Reflection
import scala.quoted.{Quotes, Type, quotes}

/**
 * Method bindings code generation.
 */
case object MethodBindings:

  /**
   * Detect valid API methods in the specified API type.
   *
   * @param ref reflection
   * @param apiType API type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return valid method descriptors or error messages by method name
   */
  def detectApiMethods[Effect[_]: Type](
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
   * Create instance method call term.
   *
   * @param quotes quototation context
   * @param instance instance term
   * @param methodName method name
   * @param typeArguments method type argument types
   * @param arguments method argument terms
   * @return instance method call term
   */
  def callMethodTerm(
    quotes: Quotes,
    instance: quotes.reflect.Term,
    methodName: String,
    typeArguments: List[quotes.reflect.TypeRepr],
    arguments: List[List[quotes.reflect.Tree]]
  ): quotes.reflect.Term =
    import quotes.reflect.{Select, Term}

    Select.unique(instance, methodName).appliedToTypes(typeArguments).appliedToArgss(
      arguments.asInstanceOf[List[List[Term]]]
    )

  /**
   * Determine whether a method uses request context as its parameter.
   * @param ref reflection context
   * @param method method
   * @tparam Context request context type
   * @return true if the method uses request context as its last parameter, false otherwise
   */
  def methodUsesContext[Context: Type](ref: Reflection, method: ref.QuotedMethod): Boolean =
    import ref.quotes.reflect.TypeRepr

    method.parameters.flatten.lastOption.exists(_.dataType =:= TypeRepr.of[Context])

  /**
   * Determine method result value type wrapped in the specified effect type.
   *
   * @param ref reflection context
   * @param method method
   * @tparam Effect effect type
   * @return result value type
   */
  def effectResultType[Effect[_]: Type](ref: Reflection, method: ref.QuotedMethod): ref.quotes.reflect.TypeRepr =
    import ref.quotes.reflect.{AppliedType, TypeRepr}

    // Determine the method result value type
    method.resultType match
      case appliedType: AppliedType if appliedType.tycon =:= TypeRepr.of[Effect] => appliedType.args.last
      case otherType                                                             => otherType

  /**
   * Create API method description.
   *
   * @param ref reflection
   * @param method method
   * @tparam ApiType API type
   * @return method description
   */
  def methodDescription[ApiType: Type](ref: Reflection, method: ref.QuotedMethod): String =
    import ref.quotes.reflect.TypeRepr

    val apiType = TypeRepr.of[ApiType].show
    val documentation = method.lift.documentation.map(_ + "\n").getOrElse("")
    s"$documentation$apiType.${method.lift.signature}"

  /**
   * Determine whether a method is a valid API method.
   *
   * @param ref reflection context
   * @param apiType API type
   * @param method method
   * @tparam Effect effect type
   * @return valid API method or an error message
   */
  private def validateApiMethod[Effect[_]: Type](
    ref: Reflection,
    apiType: ref.quotes.reflect.TypeTree,
    method: ref.QuotedMethod
  ): Either[String, ref.QuotedMethod] =
    import ref.quotes.reflect.{AppliedType, LambdaType, NamedType, TypeRepr}

    // No type parameters
    val signature = s"${apiType.show}.${method.lift.signature}"
    if method.typeParameters.nonEmpty then
      Left(s"Bound API method '$signature' must not have type parameters")

    // Callable at runtime
    else if !method.available then
      Left(s"Bound API method '$signature' must be callable at runtime")
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
        Left(s"Bound API method '$signature' must return the specified effect type '${effectType.show}'")
      else
        Right(method)
