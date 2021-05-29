package jsonrpc.codec.json.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
import ujson.Value
import upickle.Api

case object UpickleJsonMacros:

  inline def encode[Pickler <: Api, T](pickler: Pickler, value: T): Value = ${ encode('pickler, 'value) }

  private def encode[Pickler <: Api: Type, T: Type](pickler: Expr[Pickler], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Value] = '{
    val realPickler = $pickler
    val writer = summonInline[realPickler.Writer[T]]
    realPickler.writeJs($value)(using writer)
  }

  inline def decode[Pickler <: Api, T](pickler: Pickler, node: Value): T =
    ${ decode[Pickler, T]('pickler, 'node) }

  private def decode[Pickler <: Api: Type, T: Type](pickler: Expr[Pickler], node: Expr[Value])(using
    quotes: Quotes
  ): Expr[T] = '{
    val realPickler = $pickler
    val reader = summonInline[realPickler.Reader[T]]
    realPickler.read[T]($node)(using reader)
  }
