package jsonrpc.core

import scala.quoted.{Expr, Quotes, Type, quotes}
import jsonrpc.core.ScalaSupport.*

final class Reflection(val quotes: Quotes):
  import quotes.reflect.{asTerm, Flags, MethodType, PolyType, Select, Symbol, Term, TypeBounds, TypeRepr, TypeTree}

  // TODO: those case classes need to be path-dependent?
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

  private val abstractMemberFlags:Seq[Flags] =
    Seq(
      Flags.Deferred,
      Flags.Erased,
      Flags.Inline,
      Flags.Invisible,
      Flags.Macro,
      Flags.Transparent,
    )

  private val hiddenMemberFlags:Seq[Flags] =
    Seq(
      Flags.Private,
      Flags.PrivateLocal,
      Flags.Protected,
      Flags.Synthetic,
    )

  def methods(classTypeTree: TypeTree): Seq[Method] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberMethods.flatMap(method(classTypeTree, _))

  def fields(classTypeTree: TypeTree): Seq[Field] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberFields.flatMap(field(classTypeTree, _))

  def accessTerm(value: Term, name: String): Term =
    Select.unique(value, name)

  def callTerm(value: Term, name: String, typeArguments: List[TypeTree], arguments: List[List[Term]]): Term =
    accessTerm(value, name).appliedToTypeTrees(typeArguments).appliedToArgss(arguments)

  def term[T](value: Expr[T]): Term =
    value.asTerm

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
