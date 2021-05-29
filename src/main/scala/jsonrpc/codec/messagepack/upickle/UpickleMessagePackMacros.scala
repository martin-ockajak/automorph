package jsonrpc.codec.messagepack.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
import upack.Msg
import upickle.Api

case object UpickleMessagePackMacros:

  inline def encode[Parser <: Api, T](parser: Parser, value: T): Msg = ${ encode('parser, 'value) }

  private def encode[Parser <: Api: Type, T: Type](parser: Expr[Parser], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Msg] = '{
    val realParser = $parser
    val realWriter = summonInline[realParser.Writer[T]]
    realParser.writeMsg($value)(using realWriter)
  }

  inline def decode[Parser <: Api, Reader[T] <: Api#Reader[T], T](parser: Parser, reader: Reader[T], node: Msg): T =
    ${ decode[Parser, Reader, T]('parser, 'reader, 'node) }

  private def decode[Parser <: Api: Type, Reader[T] <: Api#Reader[T]: Type, T: Type](
    parser: Expr[Parser],
    reader: Expr[Reader[T]],
    node: Expr[Msg]
  )(using
    quotes: Quotes
  ): Expr[T] = '{
    val realParser = $parser
    realParser.readBinary[T]($node)(using $reader.asInstanceOf[realParser.Reader[T]])
  }
