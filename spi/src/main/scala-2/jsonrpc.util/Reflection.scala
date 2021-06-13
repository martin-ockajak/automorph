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
    RefMethod(methodSymbol.name.toString, methodSymbol.returnType, Seq(), typeParameters, true, true, methodSymbol)
//    symbolType match {
//      case methodType: MethodType =>
//        val (parameters, resultType) = methodSignature(methodType)
//        Some(RefMethod(
//          methodSymbol.name,
//          resultType,
//          parameters,
//          typeParameters,
//          publicSymbol(methodSymbol),
//          availableSymbol(methodSymbol),
//          methodSymbol
//        ))
//      case _ => None
//    }
  }
}

//  private def methodSignature(methodType: MethodType): (Seq[Seq[RefParameter]], Type) =
//    val methodTypes = LazyList.iterate(Option(methodType)) {
//      case Some(currentType) =>
//        currentType.resType match
//          case resultType: MethodType => Some(resultType)
//          case _                      => None
//      case _ => None
//    }.takeWhile(_.isDefined).flatten
//    val parameters = methodTypes.map {
//      currentType =>
//        currentType.paramNames.zip(currentType.paramTypes).map {
//          (name, dataType) => RefParameter(name, dataType, currentType.isImplicit)
//        }
//    }
//    val resultType = methodTypes.last.resType
//    (Seq(parameters*), resultType)
//
//  private def publicSymbol(symbol: Symbol): Boolean = !matchesFlags(symbol.flags, hiddenMemberFlags)
//
//  private def availableSymbol(symbol: Symbol): Boolean = !matchesFlags(symbol.flags, unavailableMemberFlags)
//
//  private def matchesFlags(flags: Flags, matchingFlags: Seq[Flags]): Boolean =
//    matchingFlags.foldLeft(false)((result, current) => result | flags.is(current))

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
      case Seq() => ""
      case values => s"[${values.mkString(", ")}]"
    }
    val parametersText = parameters.map { parameters =>
      s"(${
        parameters.map { parameter =>
          s"${parameter.name}: ${parameter.dataType}"
        }.mkString(", ")
      })"
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
