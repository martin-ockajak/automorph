package jsonrpc.core

import scala.quoted.{Expr, Quotes, Type, quotes}

final class Introspection(val quotes: Quotes):
  import reflect.{asTerm, Flags, MethodType, PolyType, Select, Symbol, Term, TypeBounds, TypeRepr, TypeTree}

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
    symbol: Symbol
  )

  val reflect = quotes.reflect
  private given Quotes = quotes

  private val abstractApiMethodFlags:Seq[Flags] =
    Seq(
      Flags.Deferred,
      Flags.Erased,
      Flags.Inline,
      Flags.Invisible,
      Flags.Macro,
      Flags.Transparent,
    )

  private val omittedApiMethodFlags:Seq[Flags] =
    Seq(
      Flags.Private,
      Flags.PrivateLocal,
      Flags.Protected,
      Flags.Synthetic,
    )

  private val baseMethodNames: Set[String] =
    val anyRefMethods = publicMethods(TypeTree.of[AnyRef])
    val productMethods = publicMethods(TypeTree.of[Product])
    (anyRefMethods ++ productMethods).map(_.name).toSet

  def publicApiMethods(classTypeTree: TypeTree, concrete: Boolean): Seq[Method] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    val methods = classSymbol.memberMethods.flatMap(symbol => methodDescriptor(classTypeTree, symbol))
    methods.filter(validMethod(_, public = true, concrete))

  def call(value: Term, methodName: String, typeArguments: List[TypeTree], arguments: List[List[Term]]): Term =
    Select.unique(value, methodName).appliedToTypeTrees(typeArguments).appliedToArgss(arguments)

  def term[T](value: Expr[T]): Term =
    value.asTerm

  private def methodDescriptor(classTypeTree: TypeTree, methodSymbol: Symbol): Option[Method] =
    classTypeTree.tpe.memberType(methodSymbol) match
      case methodType: MethodType =>
        val (params, resultType) = methodSignature(methodType)
        Some(Method(
          methodSymbol.name,
          resultType,
          params,
          Seq.empty,
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

  private def publicMethods(classTypeTree: TypeTree): Seq[Symbol] =
    val classSymbol = classTypeTree.tpe.typeSymbol
    classSymbol.memberMethods.filter{
      methodSymbol => !matchesFlags(methodSymbol.flags, omittedApiMethodFlags)
    }

  private def validMethod(method: Method, public: Boolean, concrete: Boolean): Boolean =
    method.symbol.flags.is(Flags.Method) &&
      !(public && matchesFlags(method.symbol.flags, omittedApiMethodFlags)) &&
        !baseMethodNames.contains(method.symbol.name) && (
          if concrete && matchesFlags(method.symbol.flags, abstractApiMethodFlags) then
            throw new IllegalStateException(s"Invalid API method: ${method.symbol.fullName}")
          else
            true
        )

  private def matchesFlags(flags: Flags, matchingFlags: Seq[Flags]): Boolean =
    matchingFlags.foldLeft(false)((result, current) => result | flags.is(current))
