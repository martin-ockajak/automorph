package jsonrpc.codec.json.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
import jsonrpc.core.{Method, Reflection}
import ujson.Value
import upickle.Api

case object UpickleJsonMacros:

  inline def encode[Parser <: Api, T](parser: Parser, value: T): Value = ${ encode('parser, 'value) }

  private def encode[Parser <: Api: Type, T: Type](parser: Expr[Parser], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Value] =
//    val writer = Expr.summon[Api#Writer[T]].getOrElse(throw IllegalStateException("ERROR"))
    '{
      val realParser = $parser
      val realWriter = summonInline[realParser.Writer[T]]
      realParser.writeJs($value)(using realWriter)
//      realParser.writeJs($value)(using $writer.asInstanceOf[realParser.Writer[T]])
    }

  inline def decode[Parser <: Api, T](inline parser: Parser, inline node: Value): T = ${ decode[Parser, T]('parser, 'node) }

  private def decode[Parser <: Api: Type, T: Type](parser: Expr[Parser], node: Expr[Value])(using
    quotes: Quotes
  ): Expr[T] =
    import ref.quotes.reflect.{AppliedType, TypeRepr, TypeTree}

    val ref = Reflection(quotes)

    val parserType = parser match
      case '{ $value: tpe } => TypeRepr.of[tpe]
    val readerType = ref.methods(parserType).filter(_.name == "reader").headOption.getOrElse(
      throw IllegalStateException(s"Upickle JSON parser API method not found: reader")
    ).resultType
    val valueReaderType = readerType match
      case appliedType: AppliedType => appliedType.tycon.appliedTo(TypeRepr.of[T])
      case _ => readerType
    val reader = valueReaderType.asType match
      case '[tpe] => Expr.summon[tpe].getOrElse {
        throw IllegalStateException(s"Given value not found: ${valueReaderType.show}")
      }
    '{
      val realParser = $parser
//      val realReader = summonInline[realParser.Reader[T]]
      realParser.read[T]($node)(using $reader.asInstanceOf[realParser.Reader[T]])
//      realParser.read[T]($node)(using realReader.asInstanceOf[realParser.Reader[T]])
    }
