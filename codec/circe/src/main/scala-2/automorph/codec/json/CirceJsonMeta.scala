package automorph.codec.json

import automorph.protocol.jsonrpc.Message
import io.circe.{Decoder, Encoder, Json}
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Circe JSON codec plugin code generation.
 */
private[automorph] trait CirceJsonMeta extends MessageCodec[Json] {
  implicit lazy val messageEncoder: Encoder[Message[Json]] = CirceJsonRpc.messageEncoder
  implicit lazy val messageDecoder: Decoder[Message[Json]] = CirceJsonRpc.messageDecoder

  override def encode[T](value: T): Json = macro CirceJsonMeta.encodeExpr[T]

  override def decode[T](node: Json): T = macro CirceJsonMeta.decodeExpr[T]
}

private[automorph] object CirceJsonMeta {

  def encodeExpr[T: c.WeakTypeTag](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Json] = {
    import c.universe.Quasiquote

    c.Expr[Json](q"""
      import io.circe.syntax.EncoderOps
      $value.asJson
    """)
  }

  def decodeExpr[T: c.WeakTypeTag](c: blackbox.Context)(node: c.Expr[Json]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[T](q"""
      $node.as[${weakTypeOf[T]}].toTry.get
    """)
  }
}
