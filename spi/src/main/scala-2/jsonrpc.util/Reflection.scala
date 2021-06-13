package jsonrpc.util

import scala.reflect.macros.blackbox

/**
 * Data type reflection tools.
 *
 * @param quotes quotation context
 */
case class Reflection[Context <: blackbox.Context](c: Context) {

  // All meta-programming data types are path-dependent on the compiler-generated reflection context
  import c.universe._

  case class RefParameter(
    name: String,
    dataType: Type,
    contextual: Boolean
  ) {
    def lift: Parameter = Parameter(name, dataType.typeSymbol.fullName, contextual)
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

    def lift: Method = Method(
      name,
      resultType.typeSymbol.fullName,
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

final case class Parameter(
  name: String,
  dataType: String,
  contextual: Boolean
)

final case class Method(
  name: String,
  resultType: String,
  parameters: Seq[Seq[Parameter]],
  typeParameters: Seq[Parameter],
  public: Boolean,
  available: Boolean,
  documentation: Option[String]
) {

  /** Method signature. */
  lazy val signature: String = {
    val typeParametersText = typeParameters.map { typeParameter =>
      s"${typeParameter.name}"
    } match {
      case Seq()  => ""
      case values => s"[${values.mkString(", ")}]"
    }
    val parametersText = parameters.map { parameters =>
      s"(${parameters.map { parameter =>
        s"${parameter.name}: ${parameter.dataType}"
      }.mkString(", ")})"
    }.mkString
    s"$name$typeParametersText$parametersText: $resultType"
  }
}

final case class Field(
  name: String,
  dataType: String,
  public: Boolean,
  available: Boolean,
  documentation: Option[String]
)
