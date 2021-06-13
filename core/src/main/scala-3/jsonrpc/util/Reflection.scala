package jsonrpc.util

import scala.quoted.{Expr, Quotes, Type, quotes}

/**
 * Data type reflection tools.
 *
 * @param quotes quotation context
 */
final case class Reflection(quotes: Quotes):

  // All meta-programming data types must are path-dependent on the compiler-generated quotation context
  import quotes.reflect.{Flags, MethodType, PolyType, Select, Symbol, Term, TypeBounds, TypeRepr, TypeTree, asTerm}

  final case class QuotedParameter(
    name: String,
    dataType: TypeRepr,
    contextual: Boolean
  ):
    def lift: Parameter = Parameter(name, dataType.show, contextual)

  final case class QuotedTypeParam(
    name: String,
    bounds: TypeBounds
  ):
    def lift: TypeParameter = TypeParameter(name, bounds.show)

  final case class QuotedMethod(
    name: String,
    resultType: TypeRepr,
    parameters: Seq[Seq[QuotedParameter]],
    typeParameters: Seq[QuotedTypeParam],
    public: Boolean,
    available: Boolean,
    symbol: Symbol
  ):

    def lift: Method = Method(
      name,
      resultType.show,
      parameters.map(_.map(_.lift)),
      typeParameters.map(_.lift),
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

  private def method(classType: TypeRepr, methodSymbol: Symbol): Option[QuotedMethod] =
    val (symbolType, typeParameters) = classType.memberType(methodSymbol) match
      case polyType: PolyType =>
        val typeParameters = polyType.paramNames.zip(polyType.paramBounds).map {
          (name, bounds) => QuotedTypeParam(name, bounds)
        }
        (polyType.resType, typeParameters)
      case otherType => (otherType, Seq.empty)
    symbolType match
      case methodType: MethodType =>
        val (parameters, resultType) = methodSignature(methodType)
        Some(QuotedMethod(
          methodSymbol.name,
          resultType,
          parameters,
          typeParameters,
          publicSymbol(methodSymbol),
          availableSymbol(methodSymbol),
          methodSymbol
        ))
      case _ => None

  private def methodSignature(methodType: MethodType): (Seq[Seq[QuotedParameter]], TypeRepr) =
    val methodTypes = LazyList.iterate(Option(methodType)) {
      case Some(currentType) =>
        currentType.resType match
          case resultType: MethodType => Some(resultType)
          case _                      => None
      case _ => None
    }.takeWhile(_.isDefined).flatten
    val parameters = methodTypes.map {
      currentType =>
        currentType.paramNames.zip(currentType.paramTypes).map {
          (name, dataType) => QuotedParameter(name, dataType, currentType.isImplicit)
        }
    }
    val resultType = methodTypes.last.resType
    (Seq(parameters*), resultType)

  private def field(classType: TypeRepr, fieldSymbol: Symbol): Option[QuotedField] =
    val fieldType = classType.memberType(fieldSymbol)
    Some(QuotedField(
      fieldSymbol.name,
      fieldType,
      publicSymbol(fieldSymbol),
      availableSymbol(fieldSymbol),
      fieldSymbol
    ))

  private def publicSymbol(symbol: Symbol): Boolean = !matchesFlags(symbol.flags, hiddenMemberFlags)

  private def availableSymbol(symbol: Symbol): Boolean = !matchesFlags(symbol.flags, unavailableMemberFlags)

  private def matchesFlags(flags: Flags, matchingFlags: Seq[Flags]): Boolean =
    matchingFlags.foldLeft(false)((result, current) => result | flags.is(current))
