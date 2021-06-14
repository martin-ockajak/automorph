package jsonrpc.protocol

import jsonrpc.util.Reflection
import scala.reflect.macros.blackbox.Context

/** Method bindings code introspection. */
case object MethodBindings {

  /**
   * Detect valid API methods in the specified API type.
   *
   * @param ref reflection
   * @tparam C macro context type
   * @tparam ApiType API type
   * @tparam Effect effect type
   * @return valid method descriptors or error messages by method name
   */
  def validApiMethods[C <: Context, ApiType: ref.c.WeakTypeTag, Effect: ref.c.WeakTypeTag](
    ref: Reflection[C]
  ): Seq[Either[String, ref.RefMethod]] = {
    // Filter out base data types methods
    val baseMethodNames = Seq(ref.c.weakTypeOf[AnyRef], ref.c.weakTypeOf[Product]).flatMap {
      baseType => ref.methods(baseType).filter(_.public).map(_.name)
    }.toSet
    val methods = ref.methods(ref.c.weakTypeOf[ApiType]).filter(_.public).filter {
      method =>! baseMethodNames.contains(method.name)
    }

    // Validate methods
    methods.map(method => validateApiMethod[C, ApiType, Effect](ref)(method))
  }

  /**
   * Determine whether a method uses request context as its parameter.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam Context request context type
   * @return true if the method uses request context as its last parameter, false otherwise
   */
  def methodUsesContext[C <: Context](ref: Reflection[C])(method: ref.RefMethod): Boolean =
    method.parameters.flatten.lastOption.exists { parameter =>
      parameter.contextual && parameter.dataType =:= ref.c.weakTypeOf[Context]
    }

  /**
   * Extract type wrapped in the specified wrapper type.
   *
   * @param ref reflection context
   * @param wrappedType wrapped type
   * @tparam Wrapper wrapper type
   * @return wrapped type
   */
  def unwrapType[C <: Context, Wrapper: ref.c.WeakTypeTag](ref: Reflection[C])(wrappedType: ref.c.Type): ref.c.Type =
    if (wrappedType.typeArgs.nonEmpty && wrappedType.typeConstructor =:= ref.c.weakTypeOf[Wrapper]) {
      wrappedType.typeArgs.last
    } else {
      wrappedType
    }

  /**
   * Create API method signature.
   *
   * @param ref reflection context
   * @param method method descriptor
   * @tparam C macro context type
   * @tparam ApiType API type
   * @return method description
   */
  def methodSignature[C <: Context, ApiType: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod
  ): String = s"${ref.c.weakTypeOf[ApiType].typeSymbol.fullName}.${method.lift.signature}"

  /**
   * Determine whether a method is a valid API method.
   *
   * @param ref reflection context
   * @param method method
   * @tparam C macro context type
   * @tparam ApiType API type
   * @tparam Effect effect type
   * @return valid API method or an error message
   */
  private def validateApiMethod[C <: Context, ApiType: ref.c.WeakTypeTag, Effect: ref.c.WeakTypeTag](
    ref: Reflection[C]
  )(method: ref.RefMethod): Either[String, ref.RefMethod] = {
    import ref.c.weakTypeOf

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
        // FIXME - get concrete result type constructor
        val effectType = weakTypeOf[Effect].typeConstructor
        val resultEffectType = method.resultType.typeArgs.nonEmpty && effectType =:= method.resultType.typeConstructor
//        println(method.resultType)
//        println(effectType)
//        println(effectType =:= method.resultType.typeConstructor)
        if (!resultEffectType && false) {
          Left(s"Bound API method '$signature' must return the specified effect type '${effectType.typeSymbol.fullName}'")
        } else {
          Right(method)
        }
      }
    }
  }
}
