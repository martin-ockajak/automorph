package jsonrpc.codec.json.upickle

import ujson.Value
import upickle.Api
import scala.quoted.{Expr, Quotes, Type}

object UpickleJsonMacros:
  inline def encode[Parser <: Api, T](
    parser: Parser,
    writer: Api#Writer[T],
    value: T
  ): Value = 
    ${encode('parser, 'writer, 'value)}

  private def encode[Parser <: Api: Type, T: Type](
    parser: Expr[Parser],
    writer: Expr[Api#Writer[T]],
    value: Expr[T]
  )(using quotes: Quotes): Expr[Value] =
    '{
      val realParser = $parser
      realParser.writeJs($value)(using $writer.asInstanceOf[realParser.Writer[T]])
    }
