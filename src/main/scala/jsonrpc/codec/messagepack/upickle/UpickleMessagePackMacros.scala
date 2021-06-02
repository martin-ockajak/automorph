package jsonrpc.codec.messagepack.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
import upack.Msg
import upickle.Api

case object UpickleMessagePackMacros:

  inline def encode[Pickler <: Api, T](pickler: Pickler, value: T): Msg = ${ encode[Pickler, T]('pickler, 'value) }

  private def encode[Pickler <: Api: Type, T: Type](pickler: Expr[Pickler], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Msg] = '{
    val typedPickler = $pickler
    val writer = summonInline[typedPickler.Writer[T]]
    typedPickler.writeMsg($value)(using writer)
  }

  inline def decode[Pickler <: Api, T](pickler: Pickler, node: Msg): T = ${ decode[Pickler, T]('pickler, 'node) }

  private def decode[Pickler <: Api: Type, T: Type](pickler: Expr[Pickler], node: Expr[Msg])(using
    quotes: Quotes
  ): Expr[T] = '{
    val typedPickler = $pickler
    val reader = summonInline[typedPickler.Reader[T]]
    typedPickler.readBinary[T]($node)(using reader)
  }
