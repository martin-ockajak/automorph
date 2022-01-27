package automorph.schema

import automorph.spi.RpcProtocol.InvalidRequestException
import automorph.spi.{EffectSystem, MessageCodec}
import scala.quoted.{Expr, Quotes, ToExpr, Type}
import scala.util.{Failure, Try}

object Macro:

  inline def bindings[Api <: String](api: Api): Unit =
    ${ bindingsMacro[Api]('api) }

  private def bindingsMacro[Api <: String: Type](
    api: Expr[Api]
  )(using quotes: Quotes): Expr[Unit] =
    import quotes.reflect.{Printer, Term, TypeRepr, asTerm}
    given Quotes = quotes

    val simpleCall = '{
      $api.indexOf("x", 0)
    }
    println(simpleCall.asTerm.show(using Printer.TreeShortCode))
    println(simpleCall.asTerm.show(using Printer.TreeStructure))
    val complexCall = call(
      quotes,
      api.asTerm,
      "indexOf",
      List(),
      List(List('{ "x" }.asTerm, '{ 0 }.asTerm))
    )
    println(complexCall.show(using Printer.TreeShortCode))
    println(complexCall.show(using Printer.TreeStructure))

    '{ () }

  def call(
    quotes: Quotes,
    instance: quotes.reflect.Term,
    methodName: String,
    typeArguments: List[quotes.reflect.TypeRepr],
    arguments: List[List[quotes.reflect.Tree]]
  ): quotes.reflect.Term =
    quotes.reflect.Select.unique(instance, methodName).appliedToTypes(typeArguments).appliedToArgss(
      arguments.asInstanceOf[List[List[quotes.reflect.Term]]]
  )
