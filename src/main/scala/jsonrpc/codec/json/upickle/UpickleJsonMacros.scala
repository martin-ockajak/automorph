package jsonrpc.codec.json.upickle

import ujson.Value
import upickle.Api
import scala.quoted.{Expr, Quotes, Type}
import compiletime.error

object UpickleJsonMacros:

  inline def encode[Parser <: Api, T](
    parser: Parser,
    writer: Api#Writer[T],
    value: T
  ): Value = ${ encode('parser, 'writer, 'value) }

  private def encode[Parser <: Api: Type, T: Type](
    parser: Expr[Parser],
    writer: Expr[Api#Writer[T]],
    value: Expr[T]
  )(using quotes: Quotes): Expr[Value] =
    import quotes.reflect.TypeRepr

//    val writer = Expr.summon[Api#Writer[T]] match
//      case Some(tag) => tag
//      case _ => throw new IllegalStateException(s"Unable to find given instance: ${TypeRepr.of[Api#Writer[T]].show}")
    '{
      val realParser = $parser
      realParser.writeJs($value)(using $writer.asInstanceOf[realParser.Writer[T]])
    }
