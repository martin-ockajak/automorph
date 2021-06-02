package jsonrpc.codec.json.circe

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}

case object CirceJsonMacros:

  inline def encode[Pickler, T](pickler: Pickler, value: T): Json = ${ encode[Pickler, T]('pickler, 'value) }

  private def encode[Pickler: Type, T: Type](pickler: Expr[Pickler], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Json] = '{
//    import pickler.given
    val encoder = summonInline[Encoder[T]]
    $value.asJson(using encoder)
  }

  inline def decode[Pickler, T](pickler: Pickler, node: Json): T = ${ decode[Pickler, T]('pickler, 'node) }

  private def decode[Pickler: Type, T: Type](pickler: Expr[Pickler], node: Expr[Json])(using
    quotes: Quotes
  ): Expr[T] = '{
//    import pickler.given
    val decoder = summonInline[Decoder[T]]
    $node.as[T](using decoder).toTry.get
  }
