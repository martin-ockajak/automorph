package jsonrpc.codec.json.upickle

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}
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
    import quotes.reflect.{TypeRepr, TypeTree}

    val parserType = parser match
      case '{ $value: tpe } => TypeTree.of[tpe]
    val superType = TypeTree.of[Api#Reader[T]]
    val readerType =
      '{
      val realParser = $parser
      ().asInstanceOf[realParser.Reader[T]]
      } match
        case '{ $value: tpe } => TypeTree.of[tpe]
    println(superType)
    println(superType.show)
    println()
    println(parserType)
    println(parserType.show)
    println()
    println(readerType)
    println(readerType.show)
  //    val reader = Expr.summon[Api#Reader[T]].getOrElse(throw IllegalStateException("ERROR"))
  //  val reader = Expr.summon[Api#Reader[T]].getOrElse(throw IllegalStateException("ERROR"))
    '{
      val realParser = $parser
//      val realReader = summonInline[Api#Reader[T]]
      val realReader = summonInline[realParser.Reader[T]]
      realParser.read[T]($node)(using realReader.asInstanceOf[realParser.Reader[T]])
//      realParser.read[T]($node)(using $reader.asInstanceOf[realParser.Reader[T]])
    }
