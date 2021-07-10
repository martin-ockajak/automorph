package automorph.protocol

import automorph.util.{Method, Parameter, Reflection}
import scala.annotation.nowarn
import scala.reflect.macros.blackbox

/** Method bindings introspection. */
private[automorph] case object MethodBindings {

  /**
   * Creates method quoted tree converter.
   *
   * @param ref reflection
   * @tparam C macro context type
   * @return method quoted tree converter
   */
  def methodLiftable[C <: blackbox.Context](ref: Reflection[C]): ref.c.universe.Liftable[Method] =
    new ref.c.universe.Liftable[Method] {

      import ref.c.universe.{Liftable, Quasiquote, Tree}

      implicit val parameterLiftable = new Liftable[Parameter] {

        override def apply(v: Parameter): Tree = q"""
          automorph.util.Parameter(
            ${v.name},
            ${v.dataType},
            ${v.contextual}
          )
        """
      }
      Seq(parameterLiftable)

      override def apply(v: Method): Tree = q"""
        automorph.util.Method(
          ${v.name},
          ${v.resultType},
          Seq(..${v.parameters.map(values => q"Seq(..$values)")}),
          Seq(..${v.typeParameters}),
          ${v.public},
          ${v.available},
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
  def validApiMethods[C <: blackbox.Context, ApiType: ref.c.WeakTypeTag, Effect: ref.c.WeakTypeTag](
    ref: Reflection[C]
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
   * Determines whether a method uses request context as its parameter.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam C macro context type
   * @tparam Context request context type
   * @return true if the method uses request context as its last parameter, false otherwise
   */
  def methodUsesContext[C <: blackbox.Context, Context: ref.c.WeakTypeTag](
    ref: Reflection[C]
  )(method: ref.RefMethod): Boolean =
    method.parameters.flatten.lastOption.exists { parameter =>
      parameter.contextual && parameter.dataType =:= ref.c.weakTypeOf[Context]
    }

  /**
   * Extracts type wrapped in a wrapper type.
   *
   * @param ref reflection context
   * @param wrapperType wrapper type
   * @param wrappedType wrapped type
   * @tparam C macro context type
   * @return wrapped type
   */
  @nowarn
  def unwrapType[C <: blackbox.Context, Wrapper: c.WeakTypeTag](c: C)(someType: c.Type): c.Type = {
    import c.universe.TypeRef

    val wrapperType = resultType(c)(c.weakTypeOf[Wrapper])
    val (wrapperTypeConstructor, wrapperTypeParameterIndex) = wrapperType match {
      case typeRef: TypeRef =>
        val expandedType = if (typeRef.typeArgs.nonEmpty) typeRef else {
          typeRef.etaExpand.resultType.dealias
        }
        if (expandedType.typeArgs.nonEmpty) {
          // Find constructor and type parameter index for an applied type
          (
            expandedType.typeConstructor,
            expandedType.typeArgs.indexWhere {
              case typeRef: TypeRef => typeRef.sym.isAbstract &&
                  !(typeRef =:= c.typeOf[Any] || typeRef <:< c.typeOf[AnyRef] || typeRef <:< c.typeOf[AnyVal])
              case _ => false
            }
          )
        } else {
          // Assume type reference to be single parameter type constructor
          (expandedType, 0)
        }
      // Keep any other types wrapped
      case _ => (wrapperType, -1)
    }
    if (
      wrapperTypeParameterIndex >= 0 &&
      someType.dealias.typeConstructor <:< wrapperTypeConstructor
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
  def methodSignature[C <: blackbox.Context, ApiType: ref.c.WeakTypeTag](
    ref: Reflection[C]
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
    ref: Reflection[C]
  )(method: ref.RefMethod): Either[String, ref.RefMethod] = {
    // No type parameters
    val signature = methodSignature[C, ApiType](ref)(method)
    if (method.typeParameters.nonEmpty) {
      Left(s"Bound API method '$signature' must not have type parameters")
    } else {
      // Callable at runtime
      if (!method.available) {
        Left(s"Bound API method '$signature' must be callable at runtime")
      } else {
        // Returns the effect type
        val effectTypeConstructor = ref.c.weakTypeOf[Effect].dealias.typeConstructor
        val matchingResultType =
          method.resultType.typeArgs.nonEmpty && method.resultType.dealias.typeConstructor <:< effectTypeConstructor

        // FIXME - determine concrete result type constructor instead of an abstract one
        if (!matchingResultType && false) {
          Left(s"Bound API method '$signature' must return the specified effect type '${effectTypeConstructor.typeSymbol.fullName}'")
        } else {
          Right(method)
        }
      }
    }
  }

  /**
   * Determine result type if specified type is a lambda type.
   *
   * @param q quotation context
   * @param someType some type
   * @return result type
   */
  @nowarn
  private def resultType[C <: blackbox.Context](c: C)(someType: c.Type): c.Type =
    someType.dealias match {
      case polyType: c.universe.PolyType => polyType.resultType.dealias
      case _ => someType.dealias
    }
}
