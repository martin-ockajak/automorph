package automorph.protocol

import automorph.util.{Method, Parameter, Reflection}
import scala.quoted.{quotes, Expr, Quotes, ToExpr, Type}

/** Method bindings introspection. */
private[automorph] case object MethodBindings:

  /**
   * Method quoted expression converter.
   *
   * @return method quoted expression converter
   */
  given methodToExpr: ToExpr[Method] = new ToExpr[Method]:

    given parameterToExpr: ToExpr[Parameter] = new ToExpr[Parameter]:

      override def apply(v: Parameter)(using Quotes): Expr[Parameter] = '{
        Parameter(
          ${ Expr(v.name) },
          ${ Expr(v.dataType) },
          ${ Expr(v.contextual) }
        )
      }

    override def apply(v: Method)(using Quotes): Expr[Method] = '{
      Method(
        ${ Expr(v.name) },
        ${ Expr(v.resultType) },
        ${ Expr(v.parameters) },
        ${ Expr(v.typeParameters) },
        ${ Expr(v.public) },
        ${ Expr(v.available) },
        ${ Expr(v.documentation) }
      )
    }

  /**
   * Detects valid API methods in an API type.
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
   * Determines whether a method uses request context as its parameter.
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
   * Extracts type wrapped in a wrapper type.
   *
   * @param q quotation context
   * @param someType wrapped type
   * @tparam Wrapper wrapper type
   * @return wrapped type
   */
  def unwrapType[Wrapper[_]: Type](q: Quotes)(someType: q.reflect.TypeRepr): q.reflect.TypeRepr =
    import q.reflect.{AppliedType, ParamRef, TypeRef, TypeRepr}

    val (wrapperTypeConstructor, wrapperTypeParameterIndex) =
      resultType(q)(TypeRepr.of[Wrapper]) match
        // Find constructor and type parameter index for an applied type
        case wrapperType: q.reflect.AppliedType => (
            wrapperType.tycon,
            wrapperType.args.indexWhere {
              case _: ParamRef => true
              case _ => false
            }
          )
        // Assume type reference to be single parameter type constructor
        case wrapperType: TypeRef => (wrapperType.dealias, 0)
        // Keep any other types wrapped
        case wrapperType => (wrapperType, -1)
    if wrapperTypeParameterIndex >= 0 then
      someType.dealias match
        case appliedType: AppliedType if appliedType.tycon <:< wrapperTypeConstructor =>
          appliedType.args(wrapperTypeParameterIndex)
        case _ => someType
    else someType

  /**
   * Creates a method signature.
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
   * Determines whether a method is a valid API method.
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

    // Returns the effect type
    else
      val effectType = resultType(ref.q)(TypeRepr.of[Effect])
      val matchingResultType =
        effectType.dealias match
          case appliedEffectType: AppliedType =>
            method.resultType.dealias match
              case resultType: AppliedType =>
                resultType.tycon <:< appliedEffectType.tycon
              case _ => false
          case _ => true
      if !matchingResultType then
        Left(s"Bound API method '$signature' must return the specified effect type '${effectType.show}'")
      else
        Right(method)

  /**
   * Determine result type if specified type is a lambda type.
   *
   * @param q quotation context
   * @param someType some type
   * @return result type
   */
  private def resultType(q: Quotes)(someType: q.reflect.TypeRepr): q.reflect.TypeRepr =
    someType.dealias match
      case lambdaType: q.reflect.LambdaType => lambdaType.resType.dealias
      case _ => someType.dealias
