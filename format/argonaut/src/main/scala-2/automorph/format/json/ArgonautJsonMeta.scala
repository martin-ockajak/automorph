package automorph.format.json

import argonaut.Json
import automorph.spi.MessageFormat
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Argonaut JSON format plugin code generation.
 */
private[automorph] trait ArgonautJsonMeta extends MessageFormat[Json] {

  override def encode[T](value: T): Json = macro ArgonautJsonFormatMeta.encodeExpr[T]

  override def decode[T](node: Json): T = macro ArgonautJsonFormatMeta.decodeExpr[T]
}

private[automorph] object ArgonautJsonMeta {

  def encodeExpr[T: c.WeakTypeTag](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Json] = {
    import c.universe.Quasiquote

    c.Expr[Json](q"""
      import argonaut.Argonaut.ToJsonIdentity
      implicit val noneCodecJson: argonaut.CodecJson[None.type] = automorph.format.json.ArgonautJsonFormat.noneCodecJson
      $value.asJson
    """)
  }

  def decodeExpr[T: c.WeakTypeTag](c: blackbox.Context)(node: c.Expr[Json]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[T](q"""
      import argonaut.Argonaut.ToJsonIdentity
      implicit val noneCodecJson: argonaut.CodecJson[None.type] = automorph.format.json.ArgonautJsonFormat.noneCodecJson
      $node.as[${weakTypeOf[T]}].fold(
        (errorMessage, _) => throw new IllegalArgumentException(errorMessage),
        identity
      )
    """)
  }
}
