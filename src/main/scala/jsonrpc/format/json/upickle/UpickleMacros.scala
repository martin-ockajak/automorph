package jsonrpc.format.json.upickle

import ujson.Value
import upickle.Api
import scala.quoted.{Expr, Quotes, Type}

object UpickleMacros:
  inline def xencode[A <: Api, T](inline parser: A, inline writer: Api#Writer[T], inline value: T): Value = ${xencode[A, T]('parser, 'writer, 'value)}

  private def xencode[A <: Api: Type, T: Type](parser: Expr[A], writer: Expr[Api#Writer[T]], value: Expr[T])(using quotes: Quotes): Expr[Value] =
    '{
      val realParser = $parser
      realParser.writeJs(${value})(using ${writer}.asInstanceOf[realParser.Writer[T]])
    }
