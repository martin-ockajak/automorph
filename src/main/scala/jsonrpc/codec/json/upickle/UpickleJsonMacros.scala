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

  inline def decode[Parser <: Api, Reader[T] <: Api#Reader[T], T](parser: Parser, reader: Reader[T], node: Value): T =
    ${ decode[Parser, Reader, T]('parser, 'reader, 'node) }

  private def decode[Parser <: Api: Type, Reader[T] <: Api#Reader[T]: Type, T: Type](
    parser: Expr[Parser],
    reader: Expr[Reader[T]],
    node: Expr[Value]
  )(using
    quotes: Quotes
  ): Expr[T] = '{
    val realParser = $parser
    realParser.read[T]($node)(using $reader.asInstanceOf[realParser.Reader[T]])
  }
