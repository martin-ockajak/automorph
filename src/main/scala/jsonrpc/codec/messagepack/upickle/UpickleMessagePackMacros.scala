package jsonrpc.codec.messagepack.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
import upack.Msg
import upickle.Api

object UpickleMessagePackMacros:

  inline def encode[Parser <: Api, T](parser: Parser, value: T): Msg = ${ encode('parser, 'value) }

  private def encode[Parser <: Api: Type, T: Type](parser: Expr[Parser], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Msg] = '{
    val realParser = $parser
    val realWriter = summonInline[realParser.Writer[T]]
    realParser.writeMsg($value)(using realWriter)
  }
