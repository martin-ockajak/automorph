package jsonrpc.codec.json

import io.circe.Json
import jsonrpc.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
 * Circe JSON codec plugin code generation.
 */
private[jsonrpc] trait CirceJsonCodecMeta extends Codec[Json] {
  this: CirceJsonCodec =>

  override def encode[T](value: T): Json =
    macro CirceJsonCodecMeta.encodeExpr[T]

  override def decode[T](node: Json): T =
    macro CirceJsonCodecMeta.decodeExpr[T]
}

private[jsonrpc] object CirceJsonCodecMeta {

  def encodeExpr[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Json] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Json](q"""
      import io.circe.syntax.EncoderOps
      val encoder = implicitly[io.circe.Encoder[$valueType]]
      $value.asJson(encoder)
    """)
  }

  def decodeExpr[T: c.WeakTypeTag](c: Context)(node: c.Expr[Json]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[T](q"""
      import io.circe.syntax.EncoderOps
      val decoder = implicitly[io.circe.Decoder[$valueType]]
      $node.as[$valueType](decoder).toTry.get
    """)
  }
}
