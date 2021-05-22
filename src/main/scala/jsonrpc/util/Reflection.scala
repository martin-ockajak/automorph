package jsonrpc.util

import jsonrpc.util.ValueOps.asSome
import scala.quoted.{Expr, Quotes, Type, quotes}

/**
 * Data type reflection tools.
 *
 * @param quotes quotation context
 */
final class Reflection(val quotes: Quotes):

  // All meta-programming data types must are path-dependent on the compiler-generated quotation context
  import quotes.reflect.{Flags, MethodType, PolyType, Select, Symbol, Term, TypeBounds, TypeRepr, TypeTree, asTerm}

  final case class QuotedParam(
    name: String,
    dataType: TypeRepr
  ):
    def lift: Param = Param(name, dataType.show)

  final case class QuotedTypeParam(
    name: String,
    bounds: TypeBounds
  ):
    def lift: TypeParam = TypeParam(name, bounds.show)

  final case class QuotedMethod(
    name: String,
    resultType: TypeRepr,
    params: Seq[Seq[QuotedParam]],
    typeParams: Seq[QuotedTypeParam],
    public: Boolean,
    available: Boolean,
    symbol: Symbol
  ):

    def lift: Method = Method(
      name,
      resultType.show,
      params.map(_.map(_.lift)),
      typeParams.map(_.lift),
      public = public,
      available = available,
      symbol.docstring
    )

  final case class QuotedField(
    name: String,
    dataType: TypeRepr,
    public: Boolean,
    available: Boolean,
    symbol: Symbol
  ):

    def lift: Field = Field(
      name,
      dataType.show,
      public = public,
      available = available,
      documentation = symbol.docstring
    )

  private given Quotes = quotes

  /** Unavailable class member flags. */
  private val unavailableMemberFlags = Seq(
    Flags.Erased,
    Flags.Inline,
    Flags.Invisible,
    Flags.Macro,
    Flags.Transparent
  )

  /** Hidden class member flags. */
  private val hiddenMemberFlags = Seq(
    Flags.Private,
    Flags.PrivateLocal,
    Flags.Protected,
    Flags.Synthetic
  )

  /**
   * Describe class methods within quoted context.
   *
   * @param classType class type representation
   * @return quoted class method descriptors
   */
  def methods(classType: TypeRepr): Seq[QuotedMethod] =
    classType.typeSymbol.memberMethods.flatMap(method(classType, _))

  /**
   * Describe class fields within quoted context.
   *
   * @param classType class type representation
   * @return quoted class field descriptors
   */
  def fields(classType: TypeRepr): Seq[QuotedField] =
    classType.typeSymbol.memberFields.flatMap(field(classType, _))

  /**
   * Create instance member access term.
   *
   * @param instance instance term
   * @param name member name
   * @return instance member access term
   */
  def accessTerm(instance: Term, name: String): Term = Select.unique(instance, name)

  /**
   * Create instance method call term.
   *
   * @param instance instance term
   * @param name method name
   * @param typeArguments method type argument type trees
   * @param arguments method argument terms
   * @return instance method call term
   */
  def callTerm(instance: Term, name: String, typeArguments: List[TypeTree], arguments: List[List[Term]]): Term =
    accessTerm(instance, name).appliedToTypeTrees(typeArguments).appliedToArgss(arguments)

  /**
   * Create typed expression term.
   *
   * @param expression expression
   * @tparam T expression type
   * @return typed expression term
   */
  def term[T](expression: Expr[T]): Term =
    expression.asTerm

  private def method(classType: TypeRepr, methodSymbol: Symbol): Option[QuotedMethod] =
    val (symbolType, typeParams) = classType.memberType(methodSymbol) match
      case polyType: PolyType =>
        val typeParams = polyType.paramNames.zip(polyType.paramBounds).map {
          (name, bounds) => QuotedTypeParam(name, bounds)
        }
        (polyType.resType, typeParams)
      case otherType => (otherType, Seq.empty)
    symbolType match
      case methodType: MethodType =>
        val (params, resultType) = methodSignature(methodType)
        QuotedMethod(
          methodSymbol.name,
          resultType,
          params,
          typeParams,
          publicSymbol(methodSymbol),
          availableSymbol(methodSymbol),
          methodSymbol
        ).asSome
      case _ => None

  private def methodSignature(methodType: MethodType): (Seq[Seq[QuotedParam]], TypeRepr) =
    val methodTypes = LazyList.iterate(Option(methodType)) {
      case Some(currentType) =>
        currentType.resType match
          case resultType: MethodType => resultType.asSome
          case _                      => None
      case _ => None
    }.takeWhile(_.isDefined).flatten
    val params = methodTypes.map {
      currentType =>
        currentType.paramNames.zip(currentType.paramTypes).map {
          (name, dataType) => QuotedParam(name, dataType)
        }
    }
    val resultType = methodTypes.last.resType
    (params, resultType)

  private def field(classType: TypeRepr, fieldSymbol: Symbol): Option[QuotedField] =
    val fieldType = classType.memberType(fieldSymbol)
    QuotedField(
      fieldSymbol.name,
      fieldType,
      publicSymbol(fieldSymbol),
      availableSymbol(fieldSymbol),
      fieldSymbol
    ).asSome

  private def publicSymbol(symbol: Symbol): Boolean = !matchesFlags(symbol.flags, hiddenMemberFlags)

  private def availableSymbol(symbol: Symbol): Boolean = !matchesFlags(symbol.flags, unavailableMemberFlags)

  private def matchesFlags(flags: Flags, matchingFlags: Seq[Flags]): Boolean =
    matchingFlags.foldLeft(false) { (result, current) =>
      result | flags.is(current)
    }

final case class Param(
  name: String,
  dataType: String
)

final case class TypeParam(
  name: String,
  bounds: String
)

final case class Method(
  name: String,
  resultType: String,
  params: Seq[Seq[Param]],
  typeParams: Seq[TypeParam],
  public: Boolean,
  available: Boolean,
  documentation: Option[String]
):

  def signature: String =
    val typeParamsText = typeParams.map { typeParam =>
      s"${typeParam.name}"
    } match
      case Seq()  => ""
      case values => s"[${values.mkString(", ")}]"
    val paramsText = params.map { params =>
      s"(${params.map { param =>
        s"${param.name}: ${param.dataType}"
      }.mkString(", ")})"
    }.mkString
    s"$name$typeParamsText$paramsText: $resultType"

final case class Field(
  name: String,
  dataType: String,
  public: Boolean,
  available: Boolean,
  documentation: Option[String]
)
