package jsonrpc.core

import scala.quoted.{Expr, Quotes, Type, quotes}

final class Reflection(val quotes: Quotes):
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

  def fields(classTypeTree: TypeTree): Seq[Method] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberFields.flatMap(field(classTypeTree, _))

  def callTerm(value: Term, methodName: String, typeArguments: List[TypeTree], arguments: List[List[Term]]): Term =
    Select.unique(value, methodName).appliedToTypeTrees(typeArguments).appliedToArgss(arguments)

  def term[T](value: Expr[T]): Term =
    value.asTerm

  private def method(classTypeTree: TypeTree, methodSymbol: Symbol): Option[Method] =
    classTypeTree.tpe.memberType(methodSymbol) match
      case methodType: MethodType =>
        val (params, resultType) = methodSignature(methodType)
        Some(Method(
          methodSymbol.name,
          resultType,
          params,
          Seq.empty,
          publicSymbol(methodSymbol),
          concreteSymbol(methodSymbol),
          methodSymbol
        ))
      case polyType: PolyType =>
        // TODO: remove .unapply when PolyType will be declared/fixed as Matchable
        val (typeParamNames, typeBounds, resultType) = PolyType.unapply(polyType)
        resultType match
          case actualMethodType: MethodType =>
            val (params, resultType) = methodSignature(actualMethodType)
            val typeParams = typeParamNames.zip(typeBounds).map((name, bounds) => TypeParam(name, bounds))
            Some(Method(
              methodSymbol.name,
              resultType,
              params,
              typeParams,
              publicSymbol(methodSymbol),
              concreteSymbol(methodSymbol),
              methodSymbol
            ))
          case _ => None
      case _ => None

  private def methodSignature(methodType: MethodType): (Seq[Seq[Param]], TypeRepr) =
    val methodTypes = LazyList.iterate(Option(methodType)) {
      // TODO: remove .unapply when MethodType will be declared/fixed as Matchable
      case Some(currentType) => MethodType.unapply(currentType) match
        case (_, _, resultType: MethodType) => Some(resultType)
        case _ => None
      case _ => None
    }.takeWhile(_.isDefined).flatten
    // TODO: remove .unapply when MethodType will be declared/fixed as Matchable
    val (_, _, resultType) = MethodType.unapply(methodTypes.last)
    val params = methodTypes.map { currentType =>
      val (paramNames, paramTypes, resultType) = MethodType.unapply(currentType)
      paramNames.zip(paramTypes).map((name, dataType) => Param(name, dataType))
    }
    (params, resultType)

  private def field(classTypeTree: TypeTree, fieldSymbol: Symbol): Option[Method] =
    classTypeTree.tpe.memberType(fieldSymbol) match
//      case methodType: MethodType =>
//        val (params, resultType) = methodSignature(methodType)
//        Some(Method(
//          fieldSymbol.name,
//          resultType,
//          params,
//          Seq.empty,
//          publicSymbol(methodSymbol),
//          concreteSymbol(methodSymbol),
//          fieldSymbol
//        ))
//      case polyType: PolyType =>
//        // TODO: remove .unapply when PolyType will be declared/fixed as Matchable
//        val (typeParamNames, typeBounds, resultType) = PolyType.unapply(polyType)
//        resultType match
//          case actualMethodType: MethodType =>
//            val (params, resultType) = methodSignature(actualMethodType)
//            val typeParams = typeParamNames.zip(typeBounds).map((name, bounds) => TypeParam(name, bounds))
//            Some(Method(
//              fieldSymbol.name,
//              resultType,
//              params,
//              typeParams,
//              publicSymbol(methodSymbol),
//              concreteSymbol(methodSymbol),
//              fieldSymbol
//            ))
//          case _ => None
      case _ =>
        println(fieldSymbol.name)
        println(fieldSymbol.getClass.getName)
        None

  private def publicSymbol(symbol: Symbol): Boolean =
    !matchesFlags(symbol.flags, hiddenMemberFlags)

  private def concreteSymbol(symbol: Symbol): Boolean =
    !matchesFlags(symbol.flags, abstractMemberFlags)

  private def matchesFlags(flags: Flags, matchingFlags: Seq[Flags]): Boolean =
    matchingFlags.foldLeft(false)((result, current) => result | flags.is(current))
