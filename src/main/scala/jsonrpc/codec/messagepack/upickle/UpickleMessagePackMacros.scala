package jsonrpc.codec.messagepack.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
import upack.Msg
import upickle.Api

case object UpickleMessagePackMacros:

  inline def encode[Pickler <: Api, T](pickler: Pickler, value: T): Msg = ${ encode('pickler, 'value) }

  private def encode[Pickler <: Api: Type, T: Type](pickler: Expr[Pickler], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Msg] = '{
    val realPickler = $pickler
    val writer = summonInline[realPickler.Writer[T]]
    realPickler.writeMsg($value)(using writer)
  }

  inline def decode[Pickler <: Api, T](pickler: Pickler, node: Msg): T =
    ${ decode[Pickler, T]('pickler, 'node) }

  private def decode[Pickler <: Api: Type, T: Type](pickler: Expr[Pickler], node: Expr[Msg])(using
    quotes: Quotes
  ): Expr[T] = '{
    val realPickler = $pickler
    val reader = summonInline[realPickler.Reader[T]]
    realPickler.readBinary[T]($node)(using reader)
  }
