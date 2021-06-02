package jsonrpc.codec.json.circe

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}

case object CirceJsonMacros:

  inline def encode[EncodeDecoders, T](encodeDecoders: EncodeDecoders, value: T): Json = ${ encode[EncodeDecoders, T]('encodeDecoders, 'value) }

  private def encode[EncodeDecoders: Type, T: Type](encodeDecoders: Expr[EncodeDecoders], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Json] = '{
//    import encodeDecoders.given
    val encoder = summonInline[Encoder[T]]
    $value.asJson(using encoder)
  }

  inline def decode[EncodeDecoders, T](encodeDecoders: EncodeDecoders, node: Json): T = ${ decode[EncodeDecoders, T]('encodeDecoders, 'node) }

  private def decode[EncodeDecoders: Type, T: Type](encodeDecoders: Expr[EncodeDecoders], node: Expr[Json])(using
    quotes: Quotes
  ): Expr[T] = '{
//    import encodeDecoders.given
    val decoder = summonInline[Decoder[T]]
    $node.as[T](using decoder).toTry.get
  }
