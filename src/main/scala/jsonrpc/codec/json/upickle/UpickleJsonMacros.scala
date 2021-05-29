package jsonrpc.codec.json.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
import ujson.Value
import upickle.Api

case object UpickleJsonMacros:

  inline def encode[Parser <: Api, T](parser: Parser, value: T): Value = ${ encode('parser, 'value) }

  private def encode[Parser <: Api: Type, T: Type](parser: Expr[Parser], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Value] = '{
    val realParser = $parser
    val realWriter = summonInline[realParser.Writer[T]]
    realParser.writeJs($value)(using realWriter)
  }

  inline def decode[Parser <: Api, T](parser: Parser, node: Value): T =
    ${ decode[Parser, T]('parser, 'node) }

  private def decode[Parser <: Api: Type, T: Type](parser: Expr[Parser], node: Expr[Value])(using
    quotes: Quotes
  ): Expr[T] = '{
    val realParser = $parser
    val realReader = summonInline[realParser.Reader[T]]
    realParser.read[T]($node)(using realReader)
  }
