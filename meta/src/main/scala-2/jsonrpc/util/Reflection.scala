package jsonrpc.util

import scala.reflect.macros.blackbox.Context

/**
 * Data type reflection tools.
 *
 * @tparam C macro context type
 * @param c macro context
 */
final case class Reflection[C <: Context](c: C) {

  // All meta-programming data types are path-dependent on the compiler-generated reflection context
  import c.universe._

  case class RefParameter(
    name: String,
    dataType: Type,
    contextual: Boolean
  ) {

    def lift: Parameter =
      Parameter(name, show(dataType), contextual)
  }

  case class RefMethod(
    name: String,
    resultType: Type,
    parameters: Seq[Seq[RefParameter]],
    typeParameters: Seq[RefParameter],
    public: Boolean,
    available: Boolean,
    symbol: Symbol
  ) {

    def lift: Method =
      Method(
        name,
        show(resultType),
        parameters.map(_.map(_.lift)),
        typeParameters.map(_.lift),
        public = public,
        available = available,
        documentation = None
      )
  }

  /**
   * Describe class methods within quoted context.
   *
   * @param classType class type representation
   * @return quoted class method descriptors
   */
  def methods(classType: Type): Seq[RefMethod] =
    classType.members.filter(_.isMethod).map(member => method(member.asMethod)).toSeq

  private def method(methodSymbol: MethodSymbol): RefMethod = {
    val typeParameters = methodSymbol.typeParams.map(_.asType).map { typeSymbol =>
      RefParameter(typeSymbol.name.toString, typeSymbol.toType, false)
    }
    val parameters = methodSymbol.paramLists.map(_.map { parameter =>
      RefParameter(parameter.name.toString, parameter.typeSignature, false)
    })
    RefMethod(
      methodSymbol.name.toString,
      methodSymbol.returnType,
      parameters,
      typeParameters,
      publicMethod(methodSymbol),
      availableMethod(methodSymbol),
      methodSymbol
    )
  }

  private def publicMethod(methodSymbol: MethodSymbol): Boolean =
    methodSymbol.isPublic &&
      !methodSymbol.isSynthetic

  private def availableMethod(methodSymbol: MethodSymbol): Boolean =
    !methodSymbol.isMacro
}
