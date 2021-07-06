package automorph.codec.json

import io.circe.Json
import automorph.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
 * Circe JSON codec plugin code generation.
 */
private[automorph] trait CirceJsonCodecMeta extends Codec[Json] {

  override def encode[T](value: T): Json =
    macro CirceJsonCodecMeta.encodeExpr[T]

  override def decode[T](node: Json): T =
    macro CirceJsonCodecMeta.decodeExpr[T]
}

private[automorph] object CirceJsonCodecMeta {

  def encodeExpr[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Json] = {
    import c.universe.Quasiquote

    c.Expr[Json](q"""
      import io.circe.syntax.EncoderOps
      $value.asJson
    """)
  }

  def decodeExpr[T: c.WeakTypeTag](c: Context)(node: c.Expr[Json]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[T](q"""
      $node.as[${weakTypeOf[T]}].toTry.get
    """)
  }
}
