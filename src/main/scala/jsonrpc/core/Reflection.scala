package jsonrpc.core

import scala.quoted.{Expr, Quotes, Type, quotes}
import jsonrpc.core.ScalaSupport.*

/**
 * Data type reflection tools.
 *
 * @param quotes quotation context
 */
final class Reflection(val quotes: Quotes):
  // All meta-programming data types must are path-dependent on the compiler-generated quotation context
  import quotes.reflect.{asTerm, Flags, MethodType, PolyType, Select, Symbol, Term, TypeBounds, TypeRepr, TypeTree}

  final case class Param(
    name: String,
    dataType: TypeRepr,
  )

  final case class TypeParam(
    name: String,
    typeBounds: TypeBounds,
  )

  final case class Method(
    name: String,
    resultType: TypeRepr,
    params: Seq[Seq[Param]],
    typeParams: Seq[TypeParam],
    public: Boolean,
    concrete: Boolean,
    symbol: Symbol
  )

  final case class Field(
    name: String,
    dataType: TypeRepr,
    public: Boolean,
    concrete: Boolean,
    symbol: Symbol
  )

  private given Quotes = quotes

  /**
   * Non-concrete class member flags.
   */
  private val abstractMemberFlags =
    Seq(
      Flags.Deferred,
      Flags.Erased,
      Flags.Inline,
      Flags.Invisible,
      Flags.Macro,
      Flags.Transparent,
    )

  /**
   * Non-public class member flags.
   */
  private val hiddenMemberFlags =
    Seq(
      Flags.Private,
      Flags.PrivateLocal,
      Flags.Protected,
      Flags.Synthetic,
    )

  /**
   * Describe class methods.
   *
   * @param classTypeTree class type tree
   * @return class method descriptors
   */
  def methods(classTypeTree: TypeTree): Seq[Method] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberMethods.flatMap(method(classTypeTree, _))

  /**
   * Describe class fields.
   *
   * @param classTypeTree class type tree
   * @return class field descriptors
   */
  def fields(classTypeTree: TypeTree): Seq[Field] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberFields.flatMap(field(classTypeTree, _))

  /**
   * Create instance member access term.
   *
   * @param instance instance term
   * @param name member name
   * @return instance member access term
   */
  def accessTerm(instance: Term, name: String): Term =
    Select.unique(instance, name)

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

  private def method(classTypeTree: TypeTree, methodSymbol: Symbol): Option[Method] =
    val (symbolType, typeParams) = classTypeTree.tpe.memberType(methodSymbol) match
      case polyType: PolyType =>
        val typeParams = polyType.paramNames.zip(polyType.paramBounds).map{
          (name, bounds) => TypeParam(name, bounds)
        }
        (polyType.resType, typeParams)
      case otherType => (otherType, Seq.empty)
    symbolType match
      case methodType: MethodType =>
        val (params, resultType) = methodSignature(methodType)
        Method(
          methodSymbol.name,
          resultType,
          params,
          typeParams,
          publicSymbol(methodSymbol),
          concreteSymbol(methodSymbol),
          methodSymbol
        ).some
      case _ => None

  private def methodSignature(methodType: MethodType): (Seq[Seq[Param]], TypeRepr) =
    val methodTypes = LazyList.iterate(Option(methodType)) {
      case Some(currentType) => currentType.resType match
        case resultType: MethodType => resultType.some
        case _ => None
      case _ => None
    }.takeWhile(_.isDefined).flatten
    val params = methodTypes.map {
      currentType =>
        currentType.paramNames.zip(currentType.paramTypes).map{
          (name, dataType) => Param(name, dataType)
        }
    }
    val resultType = methodTypes.last.resType
    (params, resultType)

  private def field(classTypeTree: TypeTree, fieldSymbol: Symbol): Option[Field] =
    val fieldType = classTypeTree.tpe.memberType(fieldSymbol)
    Field(
      fieldSymbol.name,
      fieldType,
      publicSymbol(fieldSymbol),
      concreteSymbol(fieldSymbol),
      fieldSymbol
    ).some

  private def publicSymbol(symbol: Symbol): Boolean =
    !matchesFlags(symbol.flags, hiddenMemberFlags)

  private def concreteSymbol(symbol: Symbol): Boolean =
    !matchesFlags(symbol.flags, abstractMemberFlags)

  private def matchesFlags(flags: Flags, matchingFlags: Seq[Flags]): Boolean =
    matchingFlags.foldLeft(false){
      (result, current) => result | flags.is(current)
    }
