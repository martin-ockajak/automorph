package automorph.codec.json

import argonaut.Json
import automorph.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
 * Argonaut JSON codec plugin code generation.
 */
private[automorph] trait ArgonautJsonCodecMeta extends Codec[Json] {

  override def encode[T](value: T): Json = macro ArgonautJsonCodecMeta.encodeExpr[T]

  override def decode[T](node: Json): T = macro ArgonautJsonCodecMeta.decodeExpr[T]
}

private[automorph] object ArgonautJsonCodecMeta {

  def encodeExpr[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Json] = {
    import c.universe.Quasiquote

    c.Expr[Json](q"""
      implicit val noneCodecJson: argonaut.CodecJson[None.type] = automorph.codec.json.ArgonautJsonCodec.noneCodecJson
      $value.asJson
    """)
  }

  def decodeExpr[T: c.WeakTypeTag](c: Context)(node: c.Expr[Json]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[T](q"""
      import argonaut.Argonaut.ToJsonIdentity
      implicit val noneCodecJson: argonaut.CodecJson[None.type] = automorph.codec.json.ArgonautJsonCodec.noneCodecJson
      $node.as[${weakTypeOf[T]}].fold(
        (errorMessage, _) => throw new IllegalArgumentException(errorMessage),
        identity
      )
    """)
  }
}
