package automorph.reflection

import automorph.spi.protocol.{RpcFunction, RpcParameter}
import scala.annotation.nowarn
import scala.reflect.macros.blackbox

/** Method introspection. */
private[automorph] object MethodReflection {

  /**
   * Creates RPC function quoted tree converter.
   *
   * @param ref reflection
   * @tparam C macro context type
   * @return method quoted tree converter
   */
  def functionLiftable[C <: blackbox.Context](ref: ClassReflection[C]): ref.c.universe.Liftable[RpcFunction] =
    new ref.c.universe.Liftable[RpcFunction] {

      import ref.c.universe.{Liftable, Quasiquote, Tree}

      @nowarn("msg=used")
      implicit val parameterLiftable: Liftable[RpcParameter] = (v: RpcParameter) => q"""
        automorph.spi.protocol.RpcParameter(
          ${v.name},
          ${v.`type`}
        )
      """

      override def apply(v: RpcFunction): Tree = q"""
        automorph.spi.protocol.RpcFunction(
          ${v.name},
          Seq(..${v.parameters}),
          ${v.resultType},
          ${v.documentation}
        )
      """
    }

  /**
   * Detects valid API methods in an API type.
   *
   * @param ref reflection
   * @tparam C macro context type
   * @tparam ApiType API type
   * @tparam Effect effect type
   * @return valid method descriptors or error messages by method name
   */
  def apiMethods[C <: blackbox.Context, ApiType: ref.c.WeakTypeTag, Effect: ref.c.WeakTypeTag](
    ref: ClassReflection[C]
  ): Seq[Either[String, ref.RefMethod]] = {
    // Omit base data type methods
    val baseMethodNames = Seq(ref.c.weakTypeOf[AnyRef], ref.c.weakTypeOf[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val methods = ref.methods(ref.c.weakTypeOf[ApiType]).filter(_.public).filter {
      method => !baseMethodNames.contains(method.name)
    }

    // Validate methods
    methods.map(method => validateApiMethod[C, ApiType, Effect](ref)(method))
  }

  /**
   * Determines whether a method accepts request context as its last parameter.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam C macro context type
   * @tparam Context message context type
   * @return true if the method accept request context as its last parameter, false otherwise
   */
  def acceptsContext[C <: blackbox.Context, Context: ref.c.WeakTypeTag](
    ref: ClassReflection[C]
  )(method: ref.RefMethod): Boolean =
    method.parameters.flatten.lastOption.exists { parameter =>
      parameter.contextual && parameter.dataType =:= ref.c.weakTypeOf[Context]
    }

  /**
   * Extracts result type wrapped in a contextual type.
   *
   * @param c macro context
   * @param someType wrapped type
   * @tparam C macro context type
   * @tparam Context message context type
   * @tparam Contextual contextual result type
   * @return contextual result type if applicable
   */
  @nowarn("msg=check")
  def contextualResult[C <: blackbox.Context, Context: c.WeakTypeTag, Contextual: c.WeakTypeTag](
    c: C
  )(someType: c.Type): Option[c.Type] = {
    import c.universe.TypeRef

    someType.dealias match {
      case typeRef: TypeRef
        if typeRef.typeConstructor <:< c.weakTypeOf[Contextual].typeConstructor &&
          typeRef.typeArgs.size > 1 &&
          typeRef.typeArgs(1) =:= c.weakTypeOf[Context] => Some(typeRef.typeArgs(0))
      case _ => None
    }
  }

  /**
   * Extracts type wrapped in a wrapper type.
   *
   * @param c macro context
   * @param someType wrapped type
   * @tparam C macro context type
   * @return wrapped type
   */
  @nowarn("msg=check")
  def unwrapType[C <: blackbox.Context, Wrapper: c.WeakTypeTag](c: C)(someType: c.Type): c.Type = {
    import c.universe.TypeRef

    val wrapperType = resultType(c)(c.weakTypeOf[Wrapper])
    val (wrapperTypeConstructor, wrapperTypeParameterIndex) = wrapperType match {
      case typeRef: TypeRef =>
        val expandedType = if (typeRef.typeArgs.nonEmpty) typeRef else typeRef.etaExpand.resultType.dealias
        if (expandedType.typeArgs.nonEmpty) {
          // Find constructor and first type parameter index for an applied type
          (
            expandedType.typeConstructor,
            expandedType.typeArgs.indexWhere {
              case typeRef: TypeRef =>
                typeRef.typeSymbol.isAbstract &&
                  !(typeRef =:= c.typeOf[Any] || typeRef <:< c.typeOf[AnyRef] || typeRef <:< c.typeOf[AnyVal])
              case _ => false
            }
          )
        } else {
          // Assume type reference to be a single parameter type constructor
          (expandedType, 0)
        }
      // Keep any other types wrapped
      case _ => (wrapperType, -1)
    }
    if (
      wrapperTypeParameterIndex >= 0 &&
      (someType.dealias.typeConstructor <:< wrapperTypeConstructor) ||
      someType.dealias.typeConstructor.typeSymbol.name == wrapperTypeConstructor.typeSymbol.name
    ) {
      someType.dealias.typeArgs(wrapperTypeParameterIndex)
    } else someType
  }

  /**
   * Creates a method signature.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam C macro context type
   * @tparam ApiType API type
   * @return method description
   */
  def signature[C <: blackbox.Context, ApiType: ref.c.WeakTypeTag](
    ref: ClassReflection[C]
  )(method: ref.RefMethod): String =
    s"${ref.c.weakTypeOf[ApiType].typeSymbol.fullName}.${method.lift.signature}"

  /**
   * Determines whether a method is a valid API method.
   *
   * @param ref reflection context
   * @param method method
   * @tparam C macro context type
   * @tparam ApiType API type
   * @tparam Effect effect type
   * @return valid API method or an error message
   */
  private def validateApiMethod[C <: blackbox.Context, ApiType: ref.c.WeakTypeTag, Effect: ref.c.WeakTypeTag](
    ref: ClassReflection[C]
  )(method: ref.RefMethod): Either[String, ref.RefMethod] = {
    // No type parameters
    val methodSignature = signature[C, ApiType](ref)(method)
    if (method.typeParameters.nonEmpty) {
      Left(s"Bound API method '$methodSignature' must not have type parameters")
    } else {
      // Callable at runtime
      if (!method.available) {
        Left(s"Bound API method '$methodSignature' must be callable at runtime")
      } else {
        // Returns the effect type
        val effectTypeConstructor = ref.c.weakTypeOf[Effect].dealias.typeConstructor
        val matchingResultType =
          method.resultType.typeArgs.nonEmpty && method.resultType.dealias.typeConstructor <:< effectTypeConstructor

        // FIXME - determine concrete result type constructor instead of an abstract one
        if (!matchingResultType && false) {
          Left(s"Bound API method '$methodSignature' must return the specified effect type '${effectTypeConstructor.typeSymbol.fullName}'")
        } else {
          Right(method)
        }
      }
    }
  }

  /**
   * Determines result type if the specified type is a lambda type.
   *
   * @param c macro context
   * @param someType some type
   * @tparam C macro context type
   * @return result type
   */
  @nowarn("msg=check")
  private def resultType[C <: blackbox.Context](c: C)(someType: c.Type): c.Type =
    someType.dealias match {
      case polyType: c.universe.PolyType => polyType.resultType.dealias
      case _ => someType.dealias
    }
}
