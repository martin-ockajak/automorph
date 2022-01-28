package automorph.schema

import automorph.spi.RpcProtocol.InvalidRequestException
import automorph.spi.{EffectSystem, MessageCodec}
import scala.quoted.{Expr, Quotes, ToExpr, Type}
import scala.util.{Failure, Try}

object Macro:

  inline def invoke[Api <: AnyRef](api: Api): Seq[Any] => Any =
    ${ invokeMacro[Api]('api) }

  private def invokeMacro[Api <: AnyRef: Type](
    api: Expr[Api]
  )(using quotes: Quotes): Expr[Seq[Any] => Any] =
    import quotes.reflect.{Printer, Term, TypeRepr, asTerm}
    given Quotes = quotes

//    val complexCall = methodCall(
//      quotes,
//      api.asTerm,
//      "charAt",
//      List(),
//      List(List('{ 0 }.asTerm))
//    )
    val complexCall = '{ (arguments: Seq[Any]) =>
      ${
        val argumentValues = List(Range(0, 3).map { argumentIndex =>
          '{ arguments(${ Expr(argumentIndex) }).asInstanceOf[String] }.asTerm
        }.toList).asInstanceOf[List[List[Term]]]
        methodCall(
          quotes,
          api.asTerm,
          "method",
          List(),
          argumentValues
        ).asExprOf[String]
      }
    }
    println()
    println(complexCall.asTerm.show(using Printer.TreeStructure))
    println()
    println(complexCall.asTerm.show(using Printer.TreeShortCode))
    println()
    complexCall
//    '{ () }

  def methodCall(
    quotes: Quotes,
    instance: quotes.reflect.Term,
    methodName: String,
    typeArguments: List[quotes.reflect.TypeRepr],
    arguments: List[List[quotes.reflect.Tree]]
  ): quotes.reflect.Term =
    quotes.reflect.Select.unique(instance, methodName).appliedToTypes(typeArguments).appliedToArgss(
      arguments.asInstanceOf[List[List[quotes.reflect.Term]]]
  )
