package automorph.util

import automorph.spi.protocol.{RpcFunction, RpcParameter}
import automorph.util.Reflection
import scala.quoted.{quotes, Expr, Quotes, ToExpr, Type}

/** Method introspection. */
private[automorph] object MethodReflection:

  /**
   * Method RPC function quoted expression converter.
   *
   * @return method quoted expression converter
   */
  given functionToExpr: ToExpr[RpcFunction] = new ToExpr[RpcFunction]:

    given parameterToExpr: ToExpr[RpcParameter] = new ToExpr[RpcParameter]:

      override def apply(v: RpcParameter)(using Quotes): Expr[RpcParameter] = '{
        RpcParameter(
          ${ Expr(v.name) },
          ${ Expr(v.dataType) }
        )
      }

    override def apply(v: RpcFunction)(using Quotes): Expr[RpcFunction] = '{
      RpcFunction(
        ${ Expr(v.name) },
        ${ Expr(v.parameters) },
        ${ Expr(v.resultType) },
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
  def apiMethods[ApiType: Type, Effect[_]: Type](ref: Reflection): Seq[Either[String, ref.RefMethod]] =
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
   * Determines whether a method uses request context as its parameter.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam Context request context type
   * @return true if the method uses request context as its last parameter, false otherwise
   */
  def usesContext[Context: Type](ref: Reflection)(method: ref.RefMethod): Boolean =
    method.parameters.flatten.lastOption.exists { parameter =>
      parameter.contextual && parameter.dataType =:= ref.q.reflect.TypeRepr.of[Context]
    }

  /**
   * Determines type constructor of the specified type.
   *
   * @param q quotation context
   * @param someType some type
   * @return type constructor
   */
  def typeConstructor(q: Quotes)(someType: q.reflect.TypeRepr): q.reflect.TypeRepr =
    import q.reflect.{AppliedType, TypeRef, TypeRepr}

    someType.dealias match
      // Find constructor for an applied type
      case appliedType: q.reflect.AppliedType => appliedType.tycon
      // Assume type reference to be a single parameter type constructor
      case typeRef: TypeRef => typeRef.dealias
      // Keep any other types wrapped
      case _ => someType

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

    val wrapperType = resultType(q)(TypeRepr.of[Wrapper])
    val (wrapperTypeConstructor, wrapperTypeParameterIndex) = wrapperType match
      // Find constructor and type parameter index for an applied type
      case appliedType: q.reflect.AppliedType => (
          appliedType.tycon,
          appliedType.args.indexWhere {
            case _: ParamRef => true
            case _ => false
          }
        )
      // Assume type reference to be a single parameter type constructor
      case typeRef: TypeRef => (typeRef.dealias, 0)
      // Keep any other types wrapped
      case _ => (wrapperType, -1)
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
  def signature[ApiType: Type](ref: Reflection)(method: ref.RefMethod): String =
    import ref.q.reflect.{Printer, TypeRepr}

    s"${TypeRepr.of[ApiType].show(using Printer.TypeReprCode)}.${method.lift.signature}"

  /**
   * Creates a method call term.
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
    val methodSignature = signature[ApiType](ref)(method)
    if method.typeParameters.nonEmpty then
      Left(s"Bound API method '$methodSignature' must not have type parameters")

    // Callable at runtime
    else if !method.available then
      Left(s"Bound API method '$methodSignature' must be callable at runtime")

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
   * Determines result type if specified type is a lambda type.
   *
   * @param q quotation context
   * @param someType some type
   * @return result type
   */
  private def resultType(q: Quotes)(someType: q.reflect.TypeRepr): q.reflect.TypeRepr =
    someType.dealias match
      case lambdaType: q.reflect.LambdaType => lambdaType.resType.dealias
      case _ => someType.dealias
