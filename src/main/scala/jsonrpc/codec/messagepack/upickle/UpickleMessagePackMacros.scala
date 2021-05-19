package jsonrpc.codec.messagepack.upickle

import upack.Msg
import upickle.Api
import scala.quoted.{Expr, Quotes, Type}

object UpickleMessagePackMacros:
  inline def encode[Parser <: Api, T](
    parser: Parser,
    writer: Api#Writer[T],
    value: T
  ): Msg =
    ${encode('parser, 'writer, 'value)}

  private def encode[Parser <: Api: Type, T: Type](
    parser: Expr[Parser],
    writer: Expr[Api#Writer[T]],
    value: Expr[T]
  )(using quotes: Quotes): Expr[Msg] =
    '{
      val realParser = $parser
      realParser.writeMsg($value)(using $writer.asInstanceOf[realParser.Writer[T]])
    }
