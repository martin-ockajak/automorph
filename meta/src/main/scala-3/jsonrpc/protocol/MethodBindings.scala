package jsonrpc.protocol

import jsonrpc.util.Reflection
import scala.quoted.{quotes, Quotes, Type}

/** Method bindings introspection. */
private[jsonrpc] case object MethodBindings:

  /**
   * Detect valid API methods in the specified API type.
   *
   * @param ref reflection
   * @tparam ApiType API type
   * @tparam Effect effect type
   * @return valid method descriptors or error messages by method name
   */
  def validApiMethods[ApiType: Type, Effect[_]: Type](ref: Reflection): Seq[Either[String, ref.RefMethod]] =
    import ref.q.reflect.TypeRepr
    given Quotes = ref.q

    // Omit base data type methods
    val baseMethodNames = Seq(TypeRepr.of[AnyRef], TypeRepr.of[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val methods = ref.methods(TypeRepr.of[ApiType]).filter(_.public).filter {
      method => !baseMethodNames.contains(method.name)
    }

    // Validate methods
    methods.map(method => validateApiMethod[ApiType, Effect](ref)(method))

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
  def call(
    quotes: Quotes,
    instance: quotes.reflect.Term,
    methodName: String,
    typeArguments: List[quotes.reflect.TypeRepr],
    arguments: List[List[quotes.reflect.Tree]]
  ): quotes.reflect.Term =
    quotes.reflect.Select.unique(instance, methodName).appliedToTypes(typeArguments).appliedToArgss(
      arguments.asInstanceOf[List[List[quotes.reflect.Term]]]
    )

  /**
   * Determine whether a method uses request context as its parameter.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam Context request context type
   * @return true if the method uses request context as its last parameter, false otherwise
   */
  def methodUsesContext[Context: Type](ref: Reflection)(method: ref.RefMethod): Boolean =
    method.parameters.flatten.lastOption.exists { parameter =>
      parameter.contextual && parameter.dataType =:= ref.q.reflect.TypeRepr.of[Context]
    }

  /**
   * Extract type wrapped in the specified wrapper type.
   *
   * @param ref reflection context
   * @param wrappedType wrapped type
   * @tparam Wrapper wrapper type
   * @return wrapped type
   */
  def unwrapType[Wrapper[_]: Type](ref: Reflection)(wrappedType: ref.q.reflect.TypeRepr): ref.q.reflect.TypeRepr =
    wrappedType match
      case appliedType: ref.q.reflect.AppliedType if appliedType.tycon =:= ref.q.reflect.TypeRepr.of[Wrapper] =>
        appliedType.args.last
      case otherType => otherType

  /**
   * Create API method signature.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam ApiType API type
   * @return method description
   */
  def methodSignature[ApiType: Type](ref: Reflection)(method: ref.RefMethod): String =
    import ref.q.reflect.{Printer, TypeRepr}

    s"${TypeRepr.of[ApiType].show(using Printer.TypeReprCode)}.${method.lift.signature}"

  /**
   * Determine whether a method is a valid API method.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam ApiType API type
   * @tparam Effect effect type
   * @return valid API method or an error message
   */
  private def validateApiMethod[ApiType: Type, Effect[_]: Type](ref: Reflection)(
    method: ref.RefMethod
  ): Either[String, ref.RefMethod] =
    import ref.q.reflect.{AppliedType, LambdaType, TypeRepr, TypeTree}

    // No type parameters
    val apiType = TypeTree.of[ApiType]
    val signature = methodSignature[ApiType](ref)(method)
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
